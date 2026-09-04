package com.tju.elm_bk.service.impl;

import com.tju.elm_bk.dto.BusinessDTO;
import com.tju.elm_bk.dto.BusinessInfoDTO;
import com.tju.elm_bk.dto.BusinessUpdateDTO;
import com.tju.elm_bk.constant.AuthorityName;
import com.tju.elm_bk.entity.Business;
import com.tju.elm_bk.entity.User;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.mapper.BusinessMapper;
import com.tju.elm_bk.mapper.OrdersMapper;
import com.tju.elm_bk.mapper.UserMapper;
import com.tju.elm_bk.result.ResultCodeEnum;
import com.tju.elm_bk.service.BusinessService;
import com.tju.elm_bk.service.BusinessPricingPolicy;
import com.tju.elm_bk.service.BusinessRecommendationPolicy;
import com.tju.elm_bk.service.CurrentUserService;
import com.tju.elm_bk.vo.BusinessSearchVO;
import com.tju.elm_bk.vo.BusinessVO;
import com.tju.elm_bk.vo.MerchantStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class BusinessServiceImpl implements BusinessService {


    private final UserMapper userMapper;
    private final OrdersMapper ordersMapper;
    private final BusinessMapper businessMapper;
    private final CurrentUserService currentUserService;
    private final BusinessPricingPolicy businessPricingPolicy;
    private final BusinessRecommendationPolicy recommendationPolicy;

    @Override
    public BusinessVO getBusinessById(Long id) {
        Business business = businessMapper.selectBusinessById(id);
        if (business == null) throw new APIException(ResultCodeEnum.BUSINESS_MISSED);
        if (!Objects.equals(business.getStatus(), 1)) {
            User user = currentUserService.requireUser();
            boolean admin = currentUserService.isAdmin(user);
            if (!admin && (user == null || !Objects.equals(business.getUserId(), user.getId()))) {
                throw new APIException(ResultCodeEnum.BUSINESS_MISSED);
            }
        }
        return businessMapper.getBusinessById(id);
    }

    @Override
    public BusinessVO updateBusiness(Long id, BusinessUpdateDTO updateDto) {
        validateUpdateRequest(id, updateDto);
        User currentUser = currentUserService.requireUser();
        boolean hasBusinessPermission = AuthorityName.BUSINESS.isGrantedTo(currentUser);
        boolean isAdmin = currentUserService.isAdmin(currentUser);

        // 如果没有 BUSINESS 权限且不是 ADMIN，则抛出权限异常
        if (!hasBusinessPermission && !isAdmin) {
            throw new APIException(ResultCodeEnum.UNAUTHORIZED);
        }
        //如果是不是管理员，且传入的商铺id不是自己的 isSelf
        boolean isSelf=isIdPresent(businessMapper.getBusinessIdsByUserId(currentUser.getId()),id);
        if(!isSelf&&!isAdmin){
            throw new APIException(ResultCodeEnum.USER_DENIED);
        }
        Business existing = businessMapper.selectBusinessById(id);
        if (existing == null) throw new APIException(ResultCodeEnum.BUSINESS_MISSED);
        validateBusinessPricing(updateDto, existing);
        updateDto.setUpdater(currentUser.getId());
        //如果不是管理员，且传入的businessOwner的username对应的user_id不是自己的--USER_DENIED
        Long ownerId = resolveRequestedOwnerId(updateDto);
        if (ownerId != null && !isAdmin && !Objects.equals(ownerId, currentUser.getId())) {
            throw new APIException(ResultCodeEnum.USER_DENIED);
        }
        if (ownerId != null) ensureEligibleBusinessOwner(ownerId);
        //执行更新操作（部分更新）
        int result = businessMapper.patchBusiness(id, updateDto);
        //如果是管理员，需要将传入的username对应的user_id传入business表的user_id
        if(isAdmin && ownerId != null){
            if (businessMapper.updateUserIdById(ownerId, id, currentUser.getId()) != 1) {
                throw new APIException(ResultCodeEnum.BUSINESS_MISSED);
            }
        }
        if (result == 0) {
            throw new APIException(ResultCodeEnum.BUSINESS_MISSED);
        }


        // 3. 重新查询完整的商户信息并返回
        return businessMapper.getBusinessById(id);
    }
    @Override
    public BusinessVO deleteBusiness(Long id) {
        User currentUser = currentUserService.requireUser();
        boolean hasBusinessPermission = AuthorityName.BUSINESS.isGrantedTo(currentUser);
        boolean isAdmin = currentUserService.isAdmin(currentUser);


        // 如果没有 BUSINESS 权限且不是 ADMIN，则抛出权限异常
        if (!hasBusinessPermission && !isAdmin) {
            throw new APIException(ResultCodeEnum.NOT_ENOUGH_PERMISSION);//权限不足
        }
        // 判断是不是自己操作自己的店铺或者管理员
        boolean isSelf=isIdPresent(businessMapper.getBusinessIdsByUserId(currentUser.getId()),id);
        if(!isSelf&&!isAdmin){
            throw new APIException(ResultCodeEnum.NOT_ENOUGH_PERMISSION);//权限不足
        }
        BusinessVO businessVo =businessMapper.getBusinessById(id);
        int result =businessMapper.deleteBusiness(id);
        if (result == 0) {
            throw new APIException(ResultCodeEnum.BUSINESS_MISSED);//商铺不存在
        }
        return businessVo;

    }

    public static boolean isIdPresent(List<Long> idList, Long targetId) {
        // 处理空列表情况
        if (idList == null || idList.isEmpty()) {
            return false;
        }
        // 处理目标ID为null的情况
        if (targetId == null) return false;
        return idList.contains(targetId);
    }
    @Override
    public BusinessVO patchBusiness(Long id, BusinessUpdateDTO updateDto) {
        validateUpdateRequest(id, updateDto);
        User currentUser = currentUserService.requireUser();
        boolean hasBusinessPermission = AuthorityName.BUSINESS.isGrantedTo(currentUser);
        boolean isAdmin = currentUserService.isAdmin(currentUser);

        // 如果没有 BUSINESS 权限且不是 ADMIN，则抛出权限异常
        if (!hasBusinessPermission && !isAdmin) {
            throw new APIException(ResultCodeEnum.NOT_ENOUGH_PERMISSION);
        }

        //如果是不是管理员，且传入的商铺id不是自己的 isSelf
        boolean isSelf=isIdPresent(businessMapper.getBusinessIdsByUserId(currentUser.getId()),id);
        if(!isSelf&&!isAdmin){
            throw new APIException(ResultCodeEnum.USER_DENIED);
        }
        Business existing = businessMapper.selectBusinessById(id);
        if (existing == null) throw new APIException(ResultCodeEnum.BUSINESS_MISSED);
        validateBusinessPricing(updateDto, existing);
        updateDto.setUpdater(currentUser.getId());
        //如果不是管理员，且传入的businessOwner的username对应的user_id不是自己的--USER_DENIED
        Long ownerId = resolveRequestedOwnerId(updateDto);
        if (ownerId != null && !isAdmin && !Objects.equals(ownerId, currentUser.getId())) {
            throw new APIException(ResultCodeEnum.USER_DENIED);
        }
        if (ownerId != null) ensureEligibleBusinessOwner(ownerId);
        //执行更新操作（部分更新）
        int result = businessMapper.patchBusiness(id, updateDto);
        //如果是管理员，需要将传入的username对应的user_id传入business表的user_id
        if(isAdmin && ownerId != null){
            if (businessMapper.updateUserIdById(ownerId, id, currentUser.getId()) != 1) {
                throw new APIException(ResultCodeEnum.BUSINESS_MISSED);
            }
        }
        if (result == 0) {
            throw new APIException(ResultCodeEnum.BUSINESS_MISSED);
        }

        return businessMapper.getBusinessById(id);
    }

    @Override
    public BusinessVO addBusiness(BusinessDTO businessDTO) {
        if (businessDTO == null || businessDTO.getBusinessOwner() == null
                || businessDTO.getBusinessOwner().getUsername() == null
                || businessDTO.getBusinessOwner().getUsername().isBlank()) {
            throw new APIException(ResultCodeEnum.PARAM_NOT_MATCHED);
        }
        validateBusinessCreation(businessDTO);
        //先查id是否在users表里面
        User currentUser = currentUserService.requireUser();
        boolean hasBusinessPermission = AuthorityName.BUSINESS.isGrantedTo(currentUser);
        boolean isAdmin = currentUserService.isAdmin(currentUser);

        // 如果没有 BUSINESS 权限且不是 ADMIN，则抛出权限异常
        if (!hasBusinessPermission && !isAdmin) {
            throw new APIException(ResultCodeEnum.NOT_ENOUGH_PERMISSION);
        }
        // 1.是商家：传入的username对应的user_id与currentUser的user_id是否一致
        // 2.是管理员：直接通过
        Long ownerId = userMapper.getUserIdByUsername(businessDTO.getBusinessOwner().getUsername().trim());
        if (ownerId == null) throw new APIException(ResultCodeEnum.USER_MISSED);
        ensureEligibleBusinessOwner(ownerId);
        boolean isSelf=ownerId.equals(currentUser.getId());
//        boolean isSelf=isIdPresent(businessMapper.getBusinessIdsByUserId(currentUser.getId()),businessDTO.getId());
        if(!isSelf&&!isAdmin){
            throw new APIException(ResultCodeEnum.USER_DENIED);
        }
        // 校验和写库必须使用同一个服务端解析出的 ownerId，不能再相信请求体里的 id。
        businessDTO.getBusinessOwner().setId(ownerId);
        businessDTO.setCreator(currentUser.getId());
        businessDTO.setUpdater(currentUser.getId());
        businessDTO.setDeleted(false);

        int result =businessMapper.insertBusiness(businessDTO);
        if (result == 0) {//这不对吧..
            throw new APIException(ResultCodeEnum.NOT_FOUND);
        }
        return businessMapper.getBusinessById(businessDTO.getId());
    }

    @Override
    public List<BusinessVO> getBusinesses() {
        List<BusinessVO> businesses = businessMapper.getBusinesses();
//        if (businesses == null) {
//            throw new APIException(ResultCodeEnum.NOT_FOUND);
//        }
        return businesses;
    }

    //搜索与筛选商铺信息
    @Override
    public List<BusinessSearchVO> getBusinessesBySearch(String keyword, boolean isScore ,boolean isSales) {
        List<BusinessSearchVO> businesses = businessMapper.searchBusinesses(keyword);
        enrichBusinessPresentations(businesses);

        // 使用 Comparator 进行排序
        Comparator<BusinessSearchVO> comparator = null;

        if (isScore && isSales) {
            // 先按评分降序，再按销量降序
            comparator = Comparator.comparing(BusinessSearchVO::getScore,
                            Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(BusinessSearchVO::getSalesCount, Comparator.reverseOrder());
        } else if (isScore) {
            // 按评分降序
            comparator = Comparator.comparing(BusinessSearchVO::getScore,
                    Comparator.nullsLast(Comparator.reverseOrder()));
        } else if (isSales) {
            // 按销量降序
            comparator = Comparator.comparing(BusinessSearchVO::getSalesCount, Comparator.reverseOrder());
        }

        if (comparator != null) {
            businesses.sort(comparator);
        }
        return businesses;
    }

    //搜索与筛选商铺信息
    @Override
    public List<BusinessSearchVO> getBusinessesInCarousel() {
        List<BusinessSearchVO> businesses = businessMapper.searchBusinesses(null);
        enrichBusinessPresentations(businesses);

        // 使用 Comparator 进行排序
        Comparator<BusinessSearchVO> comparator = null;
        comparator = Comparator.comparing(BusinessSearchVO::getScore,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(BusinessSearchVO::getSalesCount, Comparator.reverseOrder());

        businesses.sort(comparator);


        return businesses.subList(0, Math.min(3, businesses.size()));
    }

    private void enrichBusinessPresentations(List<BusinessSearchVO> businesses) {
        Set<Long> recentPurchaseIds = getRecentPurchaseIds();
        for (BusinessSearchVO business : businesses) {
            if (business.getScore() != null) {
                business.setScore(business.getScore().setScale(2, RoundingMode.HALF_UP));
            }
            recommendationPolicy.enrich(business, recentPurchaseIds.contains(business.getId()));
        }
    }

    private static final int RECENT_PURCHASE_DAYS = 30;

    private Set<Long> getRecentPurchaseIds() {
        Set<Long> ids = new HashSet<>();
        try {
            Long userId = currentUserService.optionalUser().map(User::getId).orElse(null);
            if (userId == null) return ids;
            LocalDateTime cutoff = LocalDateTime.now().minusDays(RECENT_PURCHASE_DAYS);
            List<com.tju.elm_bk.entity.Order> orders = ordersMapper.selectRecentOrdersByUserId(userId, 100);
            if (orders == null) return ids;
            orders.stream()
                    .filter(order -> order.getBusinessId() != null)
                    .filter(order -> order.getOrderState() == null || !Set.of(8, 9).contains(order.getOrderState()))
                    .filter(order -> order.getOrderDate() == null || !order.getOrderDate().isBefore(cutoff))
                    .forEach(order -> ids.add(order.getBusinessId()));
        } catch (Exception ignored) {
            // 推荐是增强能力，订单历史查询失败不应阻塞首页浏览。
        }
        return ids;
    }

    @Override
    public Integer applyForAddBusiness(Business business) {
        if (business == null || business.getBusinessName() == null || business.getBusinessName().isBlank()) {
            throw new APIException(ResultCodeEnum.PARAM_NOT_MATCHED);
        }
        businessPricingPolicy.validate(business.getStartPrice(), business.getDeliveryPrice(),
                business.getPromotionThreshold(), business.getPromotionDiscount());
        User currentUser = currentUserService.requireUser();
        boolean hasBusinessPermission = AuthorityName.BUSINESS.isGrantedTo(currentUser);
        boolean isAdmin = currentUserService.isAdmin(currentUser);

        // 如果没有 BUSINESS 权限且不是 ADMIN，则抛出权限异常
        if (!hasBusinessPermission && !isAdmin) {
            throw new APIException(ResultCodeEnum.NOT_ENOUGH_PERMISSION);
        }

        // 设置基础信息
        business.setCreator(currentUser.getId());
        business.setUpdater(currentUser.getId());
        business.setCreateTime(LocalDateTime.now());
        business.setUpdateTime(LocalDateTime.now());

        // 状态设置：管理员直接通过，普通商家需要审核
        business.setStatus(isAdmin ? 1 : 0);

        // 用户ID设置：管理员创建则必须传入userID，普通商家使用当前用户ID
        if (isAdmin) {
            // 管理员操作，必须传入userId
            if (business.getUserId() == null) {
                throw new APIException(ResultCodeEnum.USER_VALUE_MISSED);// 用户ID不能为空
            }
            User targetOwner = userMapper.findById(business.getUserId());
            if (targetOwner == null || Boolean.TRUE.equals(targetOwner.getIsDeleted())) {
                throw new APIException(ResultCodeEnum.USER_MISSED);
            }
            ensureEligibleBusinessOwner(business.getUserId());
        } else {
            // 普通商家操作，使用当前用户ID
            business.setUserId(currentUser.getId());
        }
        // 设置默认值
        business.setIs_deleted(false);
        if (business.getDeliveryPrice() == null) {
            business.setDeliveryPrice(BigDecimal.ZERO);
        }
        if (business.getStartPrice() == null) {
            business.setStartPrice(BigDecimal.ZERO);
        }

        return businessMapper.applyForAddBusiness(business);
    }

    @Override
    public List<BusinessInfoDTO> getAllActiveBusinesses() {
        List<BusinessInfoDTO> businesses = businessMapper.getAllActiveBusinesses();
        return businesses;
    }

    @Override
    public List<Business> getMerchantBusinesses(Long userId, Integer status) {
        User currentUser = currentUserService.requireUser();
        boolean admin = currentUserService.isAdmin(currentUser);
        Long targetUserId = userId == null ? currentUser.getId() : userId;
        if (!admin && !Objects.equals(targetUserId, currentUser.getId())) {
            throw new APIException(ResultCodeEnum.USER_DENIED);
        }
        return businessMapper.selectByUserIdAndStatus(targetUserId, status);
    }

    @Override
    public List<Business> listBusinessByOrderTypeId(Integer type) {
        return businessMapper.listBusinessByOrderTypeId(type);
    }

    @Override
    public List<MerchantStatsVO> getBusinessIdList() {
        return businessMapper.selectBusinessIdListByUserId(currentUserService.requireUserId());
    }

    @Override
    public BusinessVO patchBusinessOwn(Long id, BusinessUpdateDTO updateDto) {
        validateUpdateRequest(id, updateDto);
        User currentUser = currentUserService.requireUser();
        boolean hasBusinessPermission = AuthorityName.BUSINESS.isGrantedTo(currentUser);
        boolean isAdmin = currentUserService.isAdmin(currentUser);

        // 如果没有 BUSINESS 权限且不是 ADMIN，则抛出权限异常
        if (!hasBusinessPermission && !isAdmin) {
            throw new RuntimeException("权限不足，需要“商家”或“管理员”权限");
        }
        //判断是不是自己操作自己的店铺或者管理员
        boolean isSelf=isIdPresent(businessMapper.getBusinessIdsByUserId(currentUser.getId()), id);

        if(!isSelf&&!isAdmin){
            throw new APIException(ResultCodeEnum.USER_DENIED);
        }
        Business existing = businessMapper.selectBusinessById(id);
        if (existing == null) {
            throw new APIException(ResultCodeEnum.BUSINESS_MISSED);
        }
        validateBusinessPricing(updateDto, existing);
        updateDto.setUpdater(currentUser.getId());

        int result = businessMapper.patchBusiness(id, updateDto);
        if (result == 0) {
            throw new RuntimeException("更新商户信息失败，商户不存在或已被删除");
        }
        return businessMapper.getBusinessById(id);
    }

    private void validateBusinessPricing(BusinessUpdateDTO updateDto, Business existing) {
        BigDecimal startPrice = updateDto.getStartPrice() == null
                ? existing.getStartPrice()
                : updateDto.getStartPrice();
        BigDecimal deliveryPrice = updateDto.getDeliveryPrice() == null
                ? existing.getDeliveryPrice()
                : updateDto.getDeliveryPrice();
        BigDecimal threshold = updateDto.getPromotionThreshold() == null
                ? existing.getPromotionThreshold()
                : updateDto.getPromotionThreshold();
        BigDecimal discount = updateDto.getPromotionDiscount() == null
                ? existing.getPromotionDiscount()
                : updateDto.getPromotionDiscount();
        businessPricingPolicy.validate(startPrice, deliveryPrice, threshold, discount);
    }

    private void validateBusinessCreation(BusinessDTO dto) {
        if (dto.getBusinessName() == null || dto.getBusinessName().trim().isEmpty()
                || dto.getBusinessName().trim().length() > 64
                || dto.getBusinessAddress() == null || dto.getBusinessAddress().isBlank()
                || dto.getBusinessAddress().trim().length() > 255
                || dto.getOrderTypeId() == null || dto.getOrderTypeId() <= 0) {
            throw new APIException("店铺名称、地址和经营类型不能为空且必须合法");
        }
        businessPricingPolicy.validate(dto.getStartPrice(), dto.getDeliveryPrice(), dto.getPromotionThreshold(), dto.getPromotionDiscount());
    }

    private void validateUpdateRequest(Long id, BusinessUpdateDTO updateDto) {
        if (id == null || id <= 0 || updateDto == null) {
            throw new APIException(ResultCodeEnum.PARAM_NOT_MATCHED);
        }
        if (updateDto.getBusinessName() != null && updateDto.getBusinessName().trim().isEmpty()) {
            throw new APIException("店铺名称不能为空");
        }
        if (updateDto.getBusinessName() != null && updateDto.getBusinessName().trim().length() > 64) {
            throw new APIException("店铺名称不能超过64个字符");
        }
    }

    private Long resolveRequestedOwnerId(BusinessUpdateDTO updateDto) {
        if (updateDto.getBusinessOwner() == null
                || updateDto.getBusinessOwner().getUsername() == null
                || updateDto.getBusinessOwner().getUsername().isBlank()) {
            return null;
        }
        Long ownerId = userMapper.getUserIdByUsername(updateDto.getBusinessOwner().getUsername().trim());
        if (ownerId == null) throw new APIException(ResultCodeEnum.USER_MISSED);
        return ownerId;
    }

    private void ensureEligibleBusinessOwner(Long ownerId) {
        User owner = userMapper.findByUserIdWithAuthorities(ownerId);
        if (owner == null || !Boolean.TRUE.equals(owner.getActivated())
                || (!AuthorityName.BUSINESS.isGrantedTo(owner) && !AuthorityName.ADMIN.isGrantedTo(owner))) {
            throw new APIException("店铺所有者必须是已启用的商家或管理员账号");
        }
    }

}
