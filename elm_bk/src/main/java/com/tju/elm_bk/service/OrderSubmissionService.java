package com.tju.elm_bk.service;

import com.tju.elm_bk.constant.FulfillmentMode;
import com.tju.elm_bk.constant.OrderStatus;
import com.tju.elm_bk.entity.Business;
import com.tju.elm_bk.entity.DeliveryAddress;
import com.tju.elm_bk.entity.Order;
import com.tju.elm_bk.entity.OrderDetailet;
import com.tju.elm_bk.entity.UserAsset;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.mapper.AssetMapper;
import com.tju.elm_bk.mapper.BusinessMapper;
import com.tju.elm_bk.mapper.CartMapper;
import com.tju.elm_bk.mapper.DeliveryAddressMapper;
import com.tju.elm_bk.mapper.FoodMapper;
import com.tju.elm_bk.mapper.OrderDetailetMapper;
import com.tju.elm_bk.mapper.OrdersMapper;
import com.tju.elm_bk.result.ResultCodeEnum;
import com.tju.elm_bk.vo.CartItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 创建订单用例：校验商家/地址/购物车，服务端计价，预占库存并写入订单快照。
 */
@Service
@RequiredArgsConstructor
public class OrderSubmissionService {
    private static final int MAX_DISTINCT_ITEMS = 100;

    private final OrdersMapper ordersMapper;
    private final BusinessMapper businessMapper;
    private final DeliveryAddressMapper deliveryAddressMapper;
    private final CartMapper cartMapper;
    private final FoodMapper foodMapper;
    private final OrderDetailetMapper orderDetailetMapper;
    private final AssetMapper assetMapper;
    private final CurrentUserService currentUserService;
    private final OrderPricingService orderPricingService;
    private final OrderStateTransitionService orderStateTransitionService;

    @Transactional
    public Long submit(Long businessId, Long addressId, String idempotencyKey,
                       String serviceMode, List<Long> selectedFoodIds) {
        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
        Business business = requireOrderableBusiness(businessId);
        FulfillmentMode mode = requireSupportedMode(serviceMode, business);
        Long userId = currentUserService.requireUserId();

        Long existingOrderId = findExistingOrder(userId, normalizedKey);
        if (existingOrderId != null) return existingOrderId;

        DeliveryAddress address = resolveAddress(addressId, userId, mode);
        List<CartItemVO> items = selectedCartItems(userId, businessId, selectedFoodIds);
        validateCart(items);

        UserAsset assets = assetMapper.findByUserId(userId);
        OrderPricingService.OrderQuote quote = orderPricingService.quote(business, items, assets, mode);
        reserveStock(items);

        Order order = buildOrder(businessId, addressId, normalizedKey, mode, address, userId, quote);
        ordersMapper.insertOrder(order);
        orderStateTransitionService.recordCreated(order.getId(), userId);
        saveDetails(order.getId(), items, userId);
        clearSubmittedCart(userId, businessId, selectedFoodIds);
        return order.getId();
    }

    private String normalizeIdempotencyKey(String value) {
        String key = value == null ? null : value.trim();
        if (key != null && key.isEmpty()) return null;
        if (key != null && key.length() > 64) throw new APIException(ResultCodeEnum.PARAM_NOT_MATCHED);
        return key;
    }

    private Business requireOrderableBusiness(Long businessId) {
        Business business = businessMapper.selectBusinessById(businessId);
        if (business == null) throw new APIException(ResultCodeEnum.BUSINESS_MISSED);
        if (business.getStatus() != null && business.getStatus() != 1) {
            throw new APIException("商家当前未营业或尚未通过审核");
        }
        if (Boolean.FALSE.equals(business.getOperatingStatus())) {
            throw new APIException("商家当前休息中，暂时无法下单");
        }
        if (business.getDeliveryPrice() != null && business.getDeliveryPrice().signum() < 0) {
            throw new APIException("商家配送费配置异常");
        }
        return business;
    }

    private FulfillmentMode requireSupportedMode(String serviceMode, Business business) {
        FulfillmentMode mode = FulfillmentMode.fromClientValue(serviceMode);
        if (mode == FulfillmentMode.PICKUP && !Boolean.TRUE.equals(business.getDineInAvailable())) {
            throw new APIException("该商家暂不支持到店自取，请选择外送");
        }
        return mode;
    }

    private Long findExistingOrder(Long userId, String key) {
        return key == null ? null : ordersMapper.findIdByIdempotencyKey(userId, key);
    }

    private DeliveryAddress resolveAddress(Long addressId, Long userId, FulfillmentMode mode) {
        if (!mode.requiresAddress() && addressId == null) return null;
        if (mode.requiresAddress() && addressId == null) throw new APIException("外送订单请选择收货地址");
        DeliveryAddress address = deliveryAddressMapper.getDeliveryAddressById(addressId);
        if (address == null || Boolean.TRUE.equals(address.getIsDeleted())
                || !Objects.equals(address.getUserId(), userId)) {
            throw new APIException(ResultCodeEnum.ADDRESS_PERMISSION_DENIED);
        }
        // 旧客户端可能在自取时仍传地址，校验归属后明确忽略。
        return mode.requiresAddress() ? address : null;
    }

    private List<CartItemVO> selectedCartItems(Long userId, Long businessId, List<Long> selectedFoodIds) {
        List<CartItemVO> allItems = cartMapper.selectCartItems(userId, businessId);
        if (selectedFoodIds == null || selectedFoodIds.isEmpty()) return allItems;
        if (allItems == null) return Collections.emptyList();
        Set<Long> selected = new HashSet<>(selectedFoodIds);
        return allItems.stream()
                .filter(item -> item.getFoodId() != null && selected.contains(item.getFoodId()))
                .toList();
    }

    private void validateCart(List<CartItemVO> items) {
        if (items == null || items.isEmpty()) throw new APIException(ResultCodeEnum.CART_EMPTY);
        if (items.size() > MAX_DISTINCT_ITEMS) {
            throw new APIException("单笔订单商品种类不能超过" + MAX_DISTINCT_ITEMS + "种");
        }
        Map<Long, Long> quantities = new HashMap<>();
        for (CartItemVO item : items) {
            if (item.getFoodId() != null && item.getQuantity() != null) {
                quantities.merge(item.getFoodId(), item.getQuantity().longValue(), Long::sum);
            }
        }
        for (CartItemVO item : items) {
            Integer limit = item.getPurchaseLimit();
            Long total = item.getFoodId() == null ? null : quantities.get(item.getFoodId());
            if (limit != null && total != null && total > limit) {
                throw new APIException("商品“" + safeName(item) + "”单笔限购" + limit + "份");
            }
        }
    }

    private void reserveStock(List<CartItemVO> items) {
        for (CartItemVO item : items) {
            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            if (quantity <= 0 || foodMapper.decrementStock(item.getFoodId(), quantity) != 1) {
                throw new APIException("商品“" + safeName(item) + "”库存不足或已下架");
            }
        }
    }

    private Order buildOrder(Long businessId, Long addressId, String key, FulfillmentMode mode,
                             DeliveryAddress address, Long userId, OrderPricingService.OrderQuote quote) {
        LocalDateTime now = LocalDateTime.now();
        Order order = new Order();
        order.setBusinessId(businessId);
        order.setOrderDate(now);
        order.setCustomerId(userId);
        order.setIdempotencyKey(key);
        order.setAddressId(mode.requiresAddress() ? addressId : null);
        copyAddressSnapshot(order, address);
        order.setOrderState(OrderStatus.WAITING_PAYMENT.getCode());
        order.setCreator(userId);
        order.setUpdater(userId);
        order.setCreateTime(now);
        order.setUpdateTime(now);
        order.setIsDeleted(false);
        order.setOrderTotal(quote.total());
        order.setDeliveryPrice(quote.deliveryFee());
        order.setServiceMode(mode.name());
        order.setPaymentStatus("PENDING");
        return order;
    }

    private void saveDetails(Long orderId, List<CartItemVO> items, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        for (CartItemVO item : items) {
            OrderDetailet detail = new OrderDetailet();
            detail.setOrderId(orderId);
            detail.setQuantity(item.getQuantity());
            detail.setFoodId(item.getFoodId());
            detail.setFoodPrice(item.getFoodPrice().setScale(2, RoundingMode.HALF_UP));
            detail.setCreator(userId);
            detail.setUpdater(userId);
            detail.setCreateTime(now);
            detail.setUpdateTime(now);
            detail.setIsDeleted(false);
            orderDetailetMapper.saveOrderDetailPlus(detail);
        }
    }

    private void clearSubmittedCart(Long userId, Long businessId, List<Long> selectedFoodIds) {
        if (selectedFoodIds == null || selectedFoodIds.isEmpty()) {
            cartMapper.clearCart(userId, businessId);
            return;
        }
        cartMapper.clearCartItems(userId, businessId, new ArrayList<>(new HashSet<>(selectedFoodIds)));
    }

    private void copyAddressSnapshot(Order order, DeliveryAddress address) {
        if (address == null) return;
        order.setAddressSnapshot(address.getAddress());
        order.setContactNameSnapshot(address.getContactName());
        order.setContactSexSnapshot(address.getContactSex());
        order.setContactTelSnapshot(address.getContactTel());
    }

    private String safeName(CartItemVO item) {
        return item.getFoodName() == null ? "" : item.getFoodName();
    }
}
