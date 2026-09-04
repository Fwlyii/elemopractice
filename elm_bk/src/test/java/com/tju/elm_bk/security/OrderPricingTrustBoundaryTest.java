package com.tju.elm_bk.security;

import com.tju.elm_bk.dto.OrderDTO;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.entity.Business;
import com.tju.elm_bk.entity.DeliveryAddress;
import com.tju.elm_bk.entity.Order;
import com.tju.elm_bk.entity.OrderDetailet;
import com.tju.elm_bk.entity.User;
import com.tju.elm_bk.mapper.AssetMapper;
import com.tju.elm_bk.mapper.BusinessMapper;
import com.tju.elm_bk.mapper.CartMapper;
import com.tju.elm_bk.mapper.DeliveryAddressMapper;
import com.tju.elm_bk.mapper.FoodMapper;
import com.tju.elm_bk.mapper.OrderDetailetMapper;
import com.tju.elm_bk.mapper.OrderStatusHistoryMapper;
import com.tju.elm_bk.mapper.OrdersMapper;
import com.tju.elm_bk.mapper.UserMapper;
import com.tju.elm_bk.service.AssetService;
import com.tju.elm_bk.service.BusinessPricingPolicy;
import com.tju.elm_bk.service.CurrentUserService;
import com.tju.elm_bk.service.OrderPricingService;
import com.tju.elm_bk.service.OrderSettlementService;
import com.tju.elm_bk.service.OrderStateTransitionService;
import com.tju.elm_bk.service.OrderSubmissionService;
import com.tju.elm_bk.service.impl.OrderServiceImpl;
import com.tju.elm_bk.vo.AddressVO;
import com.tju.elm_bk.vo.BusinessVO;
import com.tju.elm_bk.vo.CartItemVO;
import com.tju.elm_bk.vo.OrderVO;
import com.tju.elm_bk.vo.UserVO;
import com.tju.elm_bk.websocket.WebSocketServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderPricingTrustBoundaryTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void legacyOrderRequestCannotOverrideDatabasePriceOrDeliveryFee() {
        OrdersMapper ordersMapper = mock(OrdersMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        BusinessMapper businessMapper = mock(BusinessMapper.class);
        FoodMapper foodMapper = mock(FoodMapper.class);
        OrderDetailetMapper detailMapper = mock(OrderDetailetMapper.class);
        DeliveryAddressMapper addressMapper = mock(DeliveryAddressMapper.class);
        CartMapper cartMapper = mock(CartMapper.class);
        AssetMapper assetMapper = mock(AssetMapper.class);
        OrderStatusHistoryMapper historyMapper = mock(OrderStatusHistoryMapper.class);

        CurrentUserService currentUserService = new CurrentUserService(userMapper);
        OrderPricingService pricingService = new OrderPricingService(new BusinessPricingPolicy());
        OrderStateTransitionService stateTransitionService =
                new OrderStateTransitionService(ordersMapper, historyMapper);
        OrderSubmissionService submissionService = new OrderSubmissionService(
                ordersMapper, businessMapper, addressMapper, cartMapper, foodMapper, detailMapper,
                assetMapper, currentUserService, pricingService, stateTransitionService);
        OrderSettlementService settlementService = new OrderSettlementService(
                assetMapper, ordersMapper, foodMapper, mock(AssetService.class));
        OrderServiceImpl service = new OrderServiceImpl(
                ordersMapper, businessMapper, detailMapper, mock(WebSocketServer.class),
                currentUserService, stateTransitionService, submissionService, settlementService);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("demo_user", "unused"));
        User currentUser = new User();
        currentUser.setId(11L);
        currentUser.setUsername("demo_user");
        currentUser.setIsDeleted(false);
        when(userMapper.findByUsernameWithAuthorities("demo_user")).thenReturn(currentUser);

        Business business = new Business();
        business.setId(1L);
        business.setStatus(1);
        business.setStartPrice(BigDecimal.ZERO);
        business.setDeliveryPrice(new BigDecimal("3.00"));
        when(businessMapper.selectBusinessById(1L)).thenReturn(business);

        DeliveryAddress address = new DeliveryAddress();
        address.setId(9L);
        address.setUserId(11L);
        address.setIsDeleted(false);
        address.setAddress("天津大学北洋园校区");
        when(addressMapper.getDeliveryAddressById(9L)).thenReturn(address);

        CartItemVO item = new CartItemVO();
        item.setFoodId(100L);
        item.setFoodName("数据库中的商品");
        item.setFoodPrice(new BigDecimal("20.00"));
        item.setQuantity(2);
        item.setStock(10);
        when(cartMapper.selectCartItems(11L, 1L)).thenReturn(List.of(item));
        when(foodMapper.decrementStock(100L, 2)).thenReturn(1);
        when(detailMapper.saveOrderDetailPlus(any(OrderDetailet.class))).thenReturn(1);
        doAnswer(invocation -> {
            invocation.<Order>getArgument(0).setId(88L);
            return null;
        }).when(ordersMapper).insertOrder(any(Order.class));
        when(ordersMapper.selectOrderById(88L)).thenReturn(new OrderVO());

        BusinessVO requestedBusiness = new BusinessVO();
        requestedBusiness.setId(1L);
        AddressVO requestedAddress = new AddressVO();
        requestedAddress.setId(9L);
        UserVO requestedCustomer = new UserVO();
        requestedCustomer.setUsername("demo_user");
        OrderDTO request = new OrderDTO();
        request.setBusiness(requestedBusiness);
        request.setDeliveryAddress(requestedAddress);
        request.setCustomer(requestedCustomer);
        request.setOrderTotal(new BigDecimal("0.01"));

        service.addOrder(request);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(ordersMapper).insertOrder(orderCaptor.capture());
        assertEquals(11L, orderCaptor.getValue().getCustomerId());
        assertEquals(new BigDecimal("43.00"), orderCaptor.getValue().getOrderTotal());
        assertEquals(new BigDecimal("3.00"), orderCaptor.getValue().getDeliveryPrice());

        ArgumentCaptor<OrderDetailet> detailCaptor = ArgumentCaptor.forClass(OrderDetailet.class);
        verify(detailMapper).saveOrderDetailPlus(detailCaptor.capture());
        assertEquals(new BigDecimal("20.00"), detailCaptor.getValue().getFoodPrice());

        Order pending = orderCaptor.getValue();
        pending.setOrderState(0);
        when(ordersMapper.getOrderById(88L)).thenReturn(pending);
        when(ordersMapper.updateOrderStateIfCurrent(88L, 0, 1, 11L)).thenReturn(1);
        assertThrows(APIException.class,
                () -> service.setOrderState(88L, 1, "alipay", 0, null));
        verify(ordersMapper, never()).updateOrderPayment(any(), any(), any(), any(), any(), any());
    }
}
