package com.tju.elm_bk.service.impl;

import com.tju.elm_bk.dto.BusinessDTO;
import com.tju.elm_bk.dto.BusinessInfoDTO;
import com.tju.elm_bk.dto.BusinessUpdateDTO;
import com.tju.elm_bk.entity.Business;
import com.tju.elm_bk.entity.User;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.mapper.BusinessMapper;
import com.tju.elm_bk.mapper.MerchantInteractionMapper;
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
        //如果不是管理员，且传入的businessOwner的username对应的user_id不是自己的--USER_DENIED
        Long ownerId=userMapper.getUserIdByUsername(updateDto.getBusinessOwner().getUsername());
        boolean isOwner=ownerId.equals(currentUser.getId());
        //判断是不是管理员 isAdmin
        if(!isOwner&&!isAdmin){
            throw new APIException(ResultCodeEnum.USER_DENIED);
        }
        //执行更新操作（部分更新）
        int result = businessMapper.patchBusiness(id, updateDto);
        //如果是管理员，需要将传入的username对应的user_id传入business表的user_id
        if(isAdmin){
            //根据商铺id更新user_id
            businessMapper.updateUserIdById(ownerId,id);//id是business的商铺id，更新business表的user_id为传入的username对应的user_id
        }
        if (result == 0) {
            throw new APIException(ResultCodeEnum.BUSINESS_MISSED);
        }


        // 2. 如果有商户所有者信息，更新商户所有者
        if (updateDto.getBusinessOwner() != null) {businessMapper.updateBusinessOwner(id, updateDto);}
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
        if (targetId == null) {
            return true;
        }
        return idList.contains(targetId);
    }
    @Override
    public BusinessVO patchBusiness(Long id, BusinessUpdateDTO updateDto) {
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
        //如果不是管理员，且传入的businessOwner的username对应的user_id不是自己的--USER_DENIED
        Long ownerId=userMapper.getUserIdByUsername(updateDto.getBusinessOwner().getUsername());
        boolean isOwner=ownerId.equals(currentUser.getId());
        //判断是不是管理员 isAdmin
        if(!isOwner&&!isAdmin){
            throw new APIException(ResultCodeEnum.USER_DENIED);
        }
        //执行更新操作（部分更新）
        int result = businessMapper.patchBusiness(id, updateDto);
        //如果是管理员，需要将传入的username对应的user_id传入business表的user_id
        if(isAdmin){
            //根据商铺id更新user_id
            businessMapper.updateUserIdById(ownerId,id);//id是business的商铺id，更新business表的user_id为传入的username对应的user_id
        }
        if (result == 0) {
            throw new APIException(ResultCodeEnum.BUSINESS_MISSED);
        }

        // 2. 如果有商户所有者信息，更新商户所有者
        if (updateDto.getBusinessOwner() != null) {
            //部分更新
            businessMapper.patchBusinessOwner(id, updateDto);
        }
        return businessMapper.getBusinessById(id);
    }

    @Override
    public BusinessVO addBusiness(BusinessDTO businessDTO) {
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
        boolean isSelf=userMapper.getUserIdByUsername(businessDTO.getBusinessOwner().getUsername()).equals(currentUser.getId());
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
//        System.out.println(businesses);
        // 为每个店铺计算评分与销量
        for (BusinessSearchVO business : businesses) {
            Map<String, Object> interactionCounts = businessMapper.getInteractionCounts(business.getId());
            int salesCount = businessMapper.getSalesCount(business.getId());
            Integer likeCount = interactionMapper.countLikesByMerchantId(business.getId());
            Integer collectCount = interactionMapper.countCollectionsByMerchantId(business.getId());
            // 计算评分 (点赞权重0.6，收藏权重0.4，归一化到1-5分)
            double normalizedRating = 1 + 4 * (0.6 * likeCount / (likeCount + 10.0) + 0.4 * collectCount / (collectCount + 10.0));
            BigDecimal rating = BigDecimal.valueOf(normalizedRating).setScale(2, RoundingMode.HALF_UP);
            business.setScore(rating);
//            System.out.println("Business ID: " + business.getId() +
//                    ", likeCount: " + likeCount +
//                    ", collectCount: " + collectCount +
//                    ", rawRating: " + normalizedRating);
            business.setSalesCount(salesCount);
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
        // 为每个店铺计算评分与销量
        for (BusinessSearchVO business : businesses) {
            Map<String, Object> interactionCounts = businessMapper.getInteractionCounts(business.getId());
            int salesCount = businessMapper.getSalesCount(business.getId());
            Integer likeCount = interactionMapper.countLikesByMerchantId(business.getId());
            Integer collectCount = interactionMapper.countCollectionsByMerchantId(business.getId());
            // 计算评分 (点赞权重0.6，收藏权重0.4，归一化到1-5分)
            double normalizedRating = 1 + 4 * (0.6 * likeCount / (likeCount + 10.0) + 0.4 * collectCount / (collectCount + 10.0));
            BigDecimal rating = BigDecimal.valueOf(normalizedRating).setScale(2, RoundingMode.HALF_UP);
            business.setScore(rating);
            business.setSalesCount(salesCount);
        }

        // 使用 Comparator 进行排序
        Comparator<BusinessSearchVO> comparator = null;
        comparator = Comparator.comparing(BusinessSearchVO::getScore, Comparator.reverseOrder())
                .thenComparing(BusinessSearchVO::getSalesCount, Comparator.reverseOrder());

        businesses.sort(comparator);


        return businesses.subList(0, 3);
    }

    private User getCurrentUser() {
        String username = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();
        return userService.getUserWithAuthorities(username);
    }

    @Override
    public Integer applyForAddBusiness(Business business) {
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
        int result = businessMapper.updateBusiness(id, updateDto);
        if (result == 0) {
            throw new RuntimeException("更新商户信息失败，商户不存在或已被删除");
        }

        //判断是不是自己操作自己的店铺或者管理员
        boolean isSelf=isIdPresent(businessMapper.getBusinessIdsByUserId(currentUser.getId()),updateDto.getId());

        if(!isSelf&&!isAdmin){
            throw new RuntimeException("不是该商家自己的商铺，更新失败");
        }
        // 2. 如果有商户所有者信息，更新商户所有者
        if (updateDto.getBusinessOwner() != null) {
            businessMapper.updateBusinessOwner(id, updateDto);
        }
        return businessMapper.getBusinessById(id);
    }

}
