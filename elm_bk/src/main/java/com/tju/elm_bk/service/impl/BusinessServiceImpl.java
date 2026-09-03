package com.tju.elm_bk.service.impl;

import com.tju.elm_bk.dto.BusinessDTO;
import com.tju.elm_bk.dto.BusinessInfoDTO;
import com.tju.elm_bk.dto.BusinessUpdateDTO;
import com.tju.elm_bk.entity.Business;
import com.tju.elm_bk.entity.User;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.mapper.BusinessMapper;
import com.tju.elm_bk.mapper.MerchantInteractionMapper;
import com.tju.elm_bk.mapper.OrdersMapper;
import com.tju.elm_bk.mapper.UserMapper;
import com.tju.elm_bk.result.ResultCodeEnum;
import com.tju.elm_bk.service.BusinessService;
import com.tju.elm_bk.service.UserService;
import com.tju.elm_bk.utils.SecurityUtils;
import com.tju.elm_bk.vo.BusinessSearchVO;
import com.tju.elm_bk.vo.BusinessVO;
import com.tju.elm_bk.vo.MerchantStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BusinessServiceImpl implements BusinessService {


    @Autowired
    private UserService userService;
    @Autowired
    private MerchantInteractionMapper interactionMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private OrdersMapper ordersMapper;
    @Autowired
    private final BusinessMapper businessMapper;
//    private final BusinessVoMapper businessVoMapper; // 注入MapStruct Mapper

    @Override
    public BusinessVO getBusinessById(Long id) {
        //这里需要权限检查吗
//        System.out.println("查询商家ID: " + id);
        return businessMapper.getBusinessById(id);
    }

    @Override
    public BusinessVO updateBusiness(Long id, BusinessUpdateDTO updateDto) {
        validateUpdateRequest(id, updateDto);
        User currentUser = userMapper.findByUsernameWithAuthorities(
                SecurityUtils.getCurrentUsername().orElseThrow(() -> new APIException(ResultCodeEnum.VALUE_MISSED))
        );

        // 添加 null 检查
        if (currentUser == null) {
            throw new APIException(ResultCodeEnum.NOT_FOUND);
        }

        // 权限判断 - 检查用户是否有 BUSINESS 或 ADMIN 权限
        boolean hasBusinessPermission = currentUser.getAuthorities().stream()
                .anyMatch(auth -> "BUSINESS".equals(auth.getName()));
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(auth -> "ADMIN".equals(auth.getName()));

        // 如果没有 BUSINESS 权限且不是 ADMIN，则抛出权限异常
        if (!hasBusinessPermission && !isAdmin) {
            throw new APIException(ResultCodeEnum.UNAUTHORIZED);
        }
//        //判断是不是自己操作自己的店铺或者管理员
//        boolean isSelf=isIdPresent(businessMapper.getBusinessIdsByUserId(currentUser.getId()),id);
//
//        if(!isSelf&&!isAdmin){
//            throw new APIException(ResultCodeEnum.UNAUTHORIZED);
//        }
////        System.out.println("前端--更新商家信息为: " + updateDto);
//        // 1. 更新商户基本信息
//        int result = businessMapper.updateBusiness(id, updateDto);
//        if (result == 0) {throw new APIException(ResultCodeEnum.BUSINESS_MISSED);}

        //如果是不是管理员，且传入的商铺id不是自己的 isSelf
        boolean isSelf=isIdPresent(businessMapper.getBusinessIdsByUserId(currentUser.getId()),id);
        if(!isSelf&&!isAdmin){
            throw new APIException(ResultCodeEnum.USER_DENIED);
        }
        Business existing = businessMapper.selectBusinessById(id);
        if (existing == null) throw new APIException(ResultCodeEnum.BUSINESS_MISSED);
        validateBusinessPricing(updateDto, existing);
        //如果不是管理员，且传入的businessOwner的username对应的user_id不是自己的--USER_DENIED
        Long ownerId = resolveRequestedOwnerId(updateDto);
        if (ownerId != null && !isAdmin && !Objects.equals(ownerId, currentUser.getId())) {
            throw new APIException(ResultCodeEnum.USER_DENIED);
        }
        //执行更新操作（部分更新）
        int result = businessMapper.patchBusiness(id, updateDto);
        //如果是管理员，需要将传入的username对应的user_id传入business表的user_id
        if(isAdmin && ownerId != null){
            //根据商铺id更新user_id
            businessMapper.updateUserIdById(ownerId,id);//id是business的商铺id，更新business表的user_id为传入的username对应的user_id
        }
        if (result == 0) {
            throw new APIException(ResultCodeEnum.BUSINESS_MISSED);
        }


        // 2. 如果有商户所有者信息，更新商户所有者
        if (ownerId != null) {businessMapper.updateBusinessOwner(id, updateDto);}
        // 3. 重新查询完整的商户信息并返回
        return businessMapper.getBusinessById(id);
    }
    @Override
    public BusinessVO deleteBusiness(Long id) {
        User currentUser = userMapper.findByUsernameWithAuthorities(
                SecurityUtils.getCurrentUsername().orElseThrow(() -> new APIException(ResultCodeEnum.VALUE_MISSED))
        );

        // 添加 null 检查
        if (currentUser == null) {
            throw new APIException(ResultCodeEnum.USER_MISSED);//用户不存在
        }

        // 权限判断 - 检查用户是否有 BUSINESS 或 ADMIN 权限
        boolean hasBusinessPermission = currentUser.getAuthorities().stream()
                .anyMatch(auth -> "BUSINESS".equals(auth.getName()));
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(auth -> "ADMIN".equals(auth.getName()));


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
        User currentUser = userMapper.findByUsernameWithAuthorities(
                SecurityUtils.getCurrentUsername().orElseThrow(() -> new APIException(ResultCodeEnum.USER_MISSED))
        );

        // 添加 null 检查
        if (currentUser == null) {
            throw new APIException(ResultCodeEnum.USER_MISSED);
        }

        // 权限判断 - 检查用户是否有 BUSINESS 或 ADMIN 权限
        boolean hasBusinessPermission = currentUser.getAuthorities().stream()
                .anyMatch(auth -> "BUSINESS".equals(auth.getName()));
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(auth -> "ADMIN".equals(auth.getName()));

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
        //如果不是管理员，且传入的businessOwner的username对应的user_id不是自己的--USER_DENIED
        Long ownerId = resolveRequestedOwnerId(updateDto);
        if (ownerId != null && !isAdmin && !Objects.equals(ownerId, currentUser.getId())) {
            throw new APIException(ResultCodeEnum.USER_DENIED);
        }
        //执行更新操作（部分更新）
        int result = businessMapper.patchBusiness(id, updateDto);
        //如果是管理员，需要将传入的username对应的user_id传入business表的user_id
        if(isAdmin && ownerId != null){
            //根据商铺id更新user_id
            businessMapper.updateUserIdById(ownerId,id);//id是business的商铺id，更新business表的user_id为传入的username对应的user_id
        }
        if (result == 0) {
            throw new APIException(ResultCodeEnum.BUSINESS_MISSED);
        }

        // 2. 如果有商户所有者信息，更新商户所有者
        if (ownerId != null) {
            //部分更新
            businessMapper.patchBusinessOwner(id, updateDto);
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
        User currentUser = userMapper.findByUsernameWithAuthorities(
                SecurityUtils.getCurrentUsername().orElseThrow(() -> new APIException(ResultCodeEnum.USER_MISSED))
        );

        // 添加 null 检查
        if (currentUser == null) {
            throw new APIException(ResultCodeEnum.USER_MISSED);
        }

        // 权限判断 - 检查用户是否有 BUSINESS 或 ADMIN 权限
        boolean hasBusinessPermission = currentUser.getAuthorities().stream()
                .anyMatch(auth -> "BUSINESS".equals(auth.getName()));
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(auth -> "ADMIN".equals(auth.getName()));

        // 如果没有 BUSINESS 权限且不是 ADMIN，则抛出权限异常
        if (!hasBusinessPermission && !isAdmin) {
            throw new APIException(ResultCodeEnum.NOT_ENOUGH_PERMISSION);
        }
        // 1.是商家：传入的username对应的user_id与currentUser的user_id是否一致
        // 2.是管理员：直接通过
        Long ownerId = userMapper.getUserIdByUsername(businessDTO.getBusinessOwner().getUsername().trim());
        if (ownerId == null) throw new APIException(ResultCodeEnum.USER_MISSED);
        boolean isSelf=ownerId.equals(currentUser.getId());
//        boolean isSelf=isIdPresent(businessMapper.getBusinessIdsByUserId(currentUser.getId()),businessDTO.getId());
        if(!isSelf&&!isAdmin){
            throw new APIException(ResultCodeEnum.USER_DENIED);
        }
        //-----------------------需要调用user的接口------------------!!!

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
        Set<Long> recentPurchaseIds = getRecentPurchaseIds();
//        System.out.println(businesses);
        // 为每个店铺计算评分与销量
        for (BusinessSearchVO business : businesses) {
            int salesCount = business.getSalesCount() == null
                    ? businessMapper.getSalesCount(business.getId())
                    : business.getSalesCount();
            Integer likeCount = interactionMapper.countLikesByMerchantId(business.getId());
            Integer collectCount = interactionMapper.countCollectionsByMerchantId(business.getId());
            // 计算评分 (点赞权重0.6，收藏权重0.4，归一化到1-5分)
            double normalizedRating = 1 + 4 * (0.6 * likeCount / (likeCount + 10.0) + 0.4 * collectCount / (collectCount + 10.0));
            BigDecimal rating = business.getScore() == null
                    ? BigDecimal.valueOf(normalizedRating).setScale(2, RoundingMode.HALF_UP)
                    : business.getScore().setScale(2, RoundingMode.HALF_UP);
            business.setScore(rating);
//            System.out.println("Business ID: " + business.getId() +
//                    ", likeCount: " + likeCount +
//                    ", collectCount: " + collectCount +
//                    ", rawRating: " + normalizedRating);
            business.setSalesCount(salesCount);
            populateRecommendationMetadata(business, recentPurchaseIds);
        }

        // 使用 Comparator 进行排序
        Comparator<BusinessSearchVO> comparator = null;

        if (isScore && isSales) {
            // 先按评分降序，再按销量降序
            comparator = Comparator.comparing(BusinessSearchVO::getScore, Comparator.reverseOrder())
                    .thenComparing(BusinessSearchVO::getSalesCount, Comparator.reverseOrder());
        } else if (isScore) {
            // 按评分降序
            comparator = Comparator.comparing(BusinessSearchVO::getScore, Comparator.reverseOrder());
        } else if (isSales) {
            // 按销量降序
            comparator = Comparator.comparing(BusinessSearchVO::getSalesCount, Comparator.reverseOrder());
        }

        if (comparator != null) {
            businesses.sort(comparator);
        }
//        System.out.println(businesses);
        return businesses;
    }

    //搜索与筛选商铺信息
    @Override
    public List<BusinessSearchVO> getBusinessesInCarousel() {
        List<BusinessSearchVO> businesses = businessMapper.searchBusinesses(null);
        Set<Long> recentPurchaseIds = getRecentPurchaseIds();
        // 为每个店铺计算评分与销量
        for (BusinessSearchVO business : businesses) {
            int salesCount = business.getSalesCount() == null
                    ? businessMapper.getSalesCount(business.getId())
                    : business.getSalesCount();
            Integer likeCount = interactionMapper.countLikesByMerchantId(business.getId());
            Integer collectCount = interactionMapper.countCollectionsByMerchantId(business.getId());
            // 计算评分 (点赞权重0.6，收藏权重0.4，归一化到1-5分)
            double normalizedRating = 1 + 4 * (0.6 * likeCount / (likeCount + 10.0) + 0.4 * collectCount / (collectCount + 10.0));
            BigDecimal rating = business.getScore() == null
                    ? BigDecimal.valueOf(normalizedRating).setScale(2, RoundingMode.HALF_UP)
                    : business.getScore().setScale(2, RoundingMode.HALF_UP);
            business.setScore(rating);
            business.setSalesCount(salesCount);
            populateRecommendationMetadata(business, recentPurchaseIds);
        }

        // 使用 Comparator 进行排序
        Comparator<BusinessSearchVO> comparator = null;
        comparator = Comparator.comparing(BusinessSearchVO::getScore, Comparator.reverseOrder())
                .thenComparing(BusinessSearchVO::getSalesCount, Comparator.reverseOrder());

        businesses.sort(comparator);


        return businesses.subList(0, Math.min(3, businesses.size()));
    }

    private static final int MAX_RECOMMENDATION_TAGS = 3;
    private static final int RECENT_PURCHASE_DAYS = 30;
    private static final BigDecimal GOOD_REVIEW_SCORE = new BigDecimal("4.5");

    /**
     * 首页标签与综合排序统一在后端计算。这样规则只维护一份，
     * 换 Web/小程序前端时也不会出现不同门槛、不同排序的结果。
     */
    private void populateRecommendationMetadata(BusinessSearchVO business, Set<Long> recentPurchaseIds) {
        int sales = business.getSalesCount() == null ? 0 : business.getSalesCount();
        BigDecimal score = business.getScore() == null ? BigDecimal.ZERO : business.getScore();
        List<RecommendationTag> candidates = new ArrayList<>();
        if (recentPurchaseIds.contains(business.getId())) {
            candidates.add(new RecommendationTag("上次买过", 120));
        }
        BigDecimal threshold = business.getPromotionThreshold();
        BigDecimal discount = business.getPromotionDiscount();
        boolean validPromotion = threshold != null && discount != null
                && threshold.compareTo(BigDecimal.ONE) >= 0
                && discount.compareTo(BigDecimal.ZERO) > 0
                && discount.compareTo(threshold) < 0;
        if (validPromotion) {
            candidates.add(new RecommendationTag("满" + moneyText(threshold) + "减" + moneyText(discount), 105));
        }
        boolean newBusiness = false;
        if (business.getCreateTime() != null) {
            long age = ChronoUnit.DAYS.between(business.getCreateTime(), LocalDateTime.now());
            newBusiness = age >= 0 && age <= RECENT_PURCHASE_DAYS;
            if (newBusiness) candidates.add(new RecommendationTag("新店开业", 95));
        }
        if (score.compareTo(GOOD_REVIEW_SCORE) >= 0 && sales >= 30) {
            candidates.add(new RecommendationTag(formatCount(sales) + "人爱不释手", 88));
        }
        if (score.compareTo(GOOD_REVIEW_SCORE) >= 0) {
            candidates.add(new RecommendationTag("好评如潮", 85));
        }
        if (sales >= 10) {
            candidates.add(new RecommendationTag(formatCount(sales) + "人购买", 75));
        }
        if (Boolean.TRUE.equals(business.getDineInAvailable())) {
            candidates.add(new RecommendationTag("堂食店", 65));
        }
        if (business.getDeliveryPrice() == null || business.getDeliveryPrice().compareTo(BigDecimal.ZERO) == 0) {
            candidates.add(new RecommendationTag("免配送费", 55));
        }
        if (business.getStartPrice() != null && business.getStartPrice().compareTo(new BigDecimal("20")) <= 0) {
            candidates.add(new RecommendationTag("低价起送", 35));
        }
        candidates.sort(Comparator.comparingInt(RecommendationTag::priority).reversed());
        business.setRecommendationTags(candidates.stream().limit(MAX_RECOMMENDATION_TAGS)
                .map(RecommendationTag::label).collect(Collectors.toList()));

        double recommendation = (recentPurchaseIds.contains(business.getId()) ? 120 : 0)
                + (newBusiness ? 28 : 0)
                + (validPromotion ? Math.min(24, discount.doubleValue() * 3) : 0)
                + (score.doubleValue() - 3) * 16
                + Math.min(sales, 200) * 0.08
                + (Boolean.TRUE.equals(business.getDineInAvailable()) ? 5 : 0)
                + ((business.getDeliveryPrice() == null || business.getDeliveryPrice().compareTo(BigDecimal.ZERO) == 0) ? 3 : 0);
        business.setRecommendationScore(BigDecimal.valueOf(Math.max(0, recommendation)).setScale(2, RoundingMode.HALF_UP));
    }

    private Set<Long> getRecentPurchaseIds() {
        Set<Long> ids = new HashSet<>();
        try {
            String username = SecurityUtils.getCurrentUsername().orElse(null);
            if (username == null || "anonymousUser".equals(username)) return ids;
            Long userId = userMapper.getUserIdByUsername(username);
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

    private String moneyText(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private String formatCount(int count) {
        return count >= 1000 ? String.format("%.1fk", count / 1000.0) : String.valueOf(count);
    }

    private record RecommendationTag(String label, int priority) { }

    private User getCurrentUser() {
        String username = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();
        return userService.getUserWithAuthorities(username);
    }

    @Override
    public Integer applyForAddBusiness(Business business) {
        if (business == null || business.getBusinessName() == null || business.getBusinessName().isBlank()) {
            throw new APIException(ResultCodeEnum.PARAM_NOT_MATCHED);
        }
        validateBusinessPricing(business.getStartPrice(), business.getDeliveryPrice(),
                business.getPromotionThreshold(), business.getPromotionDiscount());
        User currentUser = userMapper.findByUsernameWithAuthorities(
                SecurityUtils.getCurrentUsername().orElseThrow(() -> new APIException(ResultCodeEnum.VALUE_MISSED))
        );

        // 添加 null 检查
        if (currentUser == null) {
            throw new APIException(ResultCodeEnum.NOT_FOUND);
        }

        // 权限判断 - 检查用户是否有 BUSINESS 或 ADMIN 权限
        boolean hasBusinessPermission = currentUser.getAuthorities().stream()
                .anyMatch(auth -> "BUSINESS".equals(auth.getName()));
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(auth -> "ADMIN".equals(auth.getName()));

        // 如果没有 BUSINESS 权限且不是 ADMIN，则抛出权限异常
        if (!hasBusinessPermission && !isAdmin) {
            throw new APIException(ResultCodeEnum.NOT_ENOUGH_PERMISSION);
        }

        // 设置基础信息
        business.setCreator(currentUser.getId());
        business.setCreateTime(LocalDateTime.now());

        // 状态设置：管理员直接通过，普通商家需要审核
        business.setStatus(isAdmin ? 1 : 0);

        // 用户ID设置：管理员创建则必须传入userID，普通商家使用当前用户ID
        if (isAdmin) {
            // 管理员操作，必须传入userId
            if (business.getUserId() == null) {
                throw new APIException(ResultCodeEnum.USER_VALUE_MISSED);// 用户ID不能为空
            }
        } else {
            // 普通商家操作，使用当前用户ID
            business.setUserId(currentUser.getId());
        }
        // 设置默认值
        if (business.getIs_deleted() == null) {
            business.setIs_deleted(false);
        }
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
        return businessMapper.selectByUserIdAndStatus(userId, status);
    }

    @Override
    public List<Business> listBusinessByOrderTypeId(Integer type) {
        return businessMapper.listBusinessByOrderTypeId(type);
    }

    @Override
    public List<MerchantStatsVO> getBusinessIdList() {
        User currentUser = userMapper.findByUsernameWithAuthorities(
                SecurityUtils.getCurrentUsername().orElseThrow(() -> new APIException(ResultCodeEnum.VALUE_MISSED))
        );

        return businessMapper.selectBusinessIdListByUserId(currentUser.getId());
    }

    @Override
    public BusinessVO patchBusinessOwn(Long id, BusinessUpdateDTO updateDto) {
        validateUpdateRequest(id, updateDto);
        User currentUser = userMapper.findByUsernameWithAuthorities(
                SecurityUtils.getCurrentUsername().orElseThrow(() -> new APIException(ResultCodeEnum.VALUE_MISSED))
        );

        // 添加 null 检查
        if (currentUser == null) {
            throw new RuntimeException("无法获取当前用户信息");
        }

        // 权限判断 - 检查用户是否有 BUSINESS 或 ADMIN 权限
        boolean hasBusinessPermission = currentUser.getAuthorities().stream()
                .anyMatch(auth -> "BUSINESS".equals(auth.getName()));
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(auth -> "ADMIN".equals(auth.getName()));

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

        int result = businessMapper.patchBusiness(id, updateDto);
        if (result == 0) {
            throw new RuntimeException("更新商户信息失败，商户不存在或已被删除");
        }
        // 2. 如果有商户所有者信息，更新商户所有者
        if (updateDto.getBusinessOwner() != null) {
            businessMapper.updateBusinessOwner(id, updateDto);
        }
        return businessMapper.getBusinessById(id);
    }

    private void validateBusinessPricing(BusinessUpdateDTO updateDto, Business existing) {
        double startPrice = updateDto.getStartPrice() == null
                ? (existing.getStartPrice() == null ? 0D : existing.getStartPrice().doubleValue())
                : updateDto.getStartPrice();
        double deliveryPrice = updateDto.getDeliveryPrice() == null
                ? (existing.getDeliveryPrice() == null ? 0D : existing.getDeliveryPrice().doubleValue())
                : updateDto.getDeliveryPrice();
        validateBusinessPricing(startPrice, deliveryPrice, updateDto.getPromotionThreshold(), updateDto.getPromotionDiscount());
    }

    private void validateBusinessCreation(BusinessDTO dto) {
        if (dto.getBusinessName() == null || dto.getBusinessName().trim().isEmpty()) {
            throw new APIException("店铺名称不能为空");
        }
        validateBusinessPricing(dto.getStartPrice(), dto.getDeliveryPrice(), dto.getPromotionThreshold(), dto.getPromotionDiscount());
    }

    private void validateBusinessPricing(java.math.BigDecimal startPrice, java.math.BigDecimal deliveryPrice,
                                         java.math.BigDecimal threshold, java.math.BigDecimal discount) {
        double start = startPrice == null ? 0D : startPrice.doubleValue();
        double delivery = deliveryPrice == null ? 0D : deliveryPrice.doubleValue();
        validateBusinessPricing(start, delivery,
                threshold == null ? null : threshold.doubleValue(),
                discount == null ? null : discount.doubleValue());
    }

    private void validateBusinessPricing(Double startPrice, Double deliveryPrice, Double threshold, Double discount) {
        double start = startPrice == null ? 0D : startPrice;
        double delivery = deliveryPrice == null ? 0D : deliveryPrice;
        if (!Double.isFinite(start) || start < 0 || !Double.isFinite(delivery) || delivery < 0) {
            throw new APIException("起送价和配送费必须为非负数字");
        }
        if (threshold != null || discount != null) {
            double min = threshold == null ? 0D : threshold;
            double off = discount == null ? 0D : discount;
            if (!Double.isFinite(min) || !Double.isFinite(off) || min < 0 || off < 0
                    || (min == 0 && off > 0) || (min > 0 && off >= min)) {
                throw new APIException("满减优惠配置不合法：优惠金额必须小于门槛");
            }
        }
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

}
