// MerchantInteractionServiceImpl.java
package com.tju.elm_bk.service.impl;

import com.tju.elm_bk.entity.MerchantInteraction;
import com.tju.elm_bk.mapper.BusinessMapper;
import com.tju.elm_bk.mapper.MerchantInteractionMapper;
import com.tju.elm_bk.mapper.UserMapper;
import com.tju.elm_bk.service.MerchantInteractionService;
import com.tju.elm_bk.vo.BusinessSearchVO;
import com.tju.elm_bk.vo.BusinessVO;
import com.tju.elm_bk.vo.MerchantInteractionVO;
import com.tju.elm_bk.vo.MerchantStatsVO;
import com.tju.elm_bk.dto.MerchantInteractionDTO;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.result.ResultCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class MerchantInteractionServiceImpl implements MerchantInteractionService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private MerchantInteractionMapper interactionMapper;
    @Autowired
    private BusinessMapper businessMapper;

    @Override
    @Transactional
    public void updateInteraction(MerchantInteractionDTO dto) {
        try {
            // 参数验证
            if (dto.getUserId() == null) {
                throw new APIException(ResultCodeEnum.PARAM_NOT_MATCHED);
            }
            if (dto.getMerchantId() == null) {
                throw new APIException(ResultCodeEnum.PARAM_NOT_MATCHED);
            }

            // 查询现有记录
            MerchantInteraction interaction = interactionMapper.selectByUserAndMerchant(
                    dto.getUserId(), dto.getMerchantId());

            if (interaction == null) {
                // 创建新记录
                interaction = new MerchantInteraction();
                interaction.setUserId(dto.getUserId());
                interaction.setMerchantId(dto.getMerchantId());
                interaction.setLiked(dto.getLiked() != null ? dto.getLiked() : false);
                interaction.setCollected(dto.getCollected() != null ? dto.getCollected() : false);
                interactionMapper.insert(interaction);
            } else {
                // 更新现有记录
                if (dto.getLiked() != null) {
                    interaction.setLiked(dto.getLiked());
                }
                if (dto.getCollected() != null) {
                    interaction.setCollected(dto.getCollected());
                }
                interactionMapper.update(interaction);
            }

            log.info("用户{}对商家{}的互动状态更新成功", dto.getUserId(), dto.getMerchantId());

        } catch (APIException e) {
            log.warn("业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("更新用户商家互动失败: userId={}, merchantId={}", dto.getUserId(), dto.getMerchantId(), e);
            throw new APIException(ResultCodeEnum.SERVER_ERROR);
        }
    }

//    @Override
//    public List<BusinessSearchVO> getUserCollections(Long userId) {
//        try {
//            if (userId == null) {
//                throw new APIException(ResultCodeEnum.PARAM_NOT_MATCHED);
//            }
//            // 检查用户是否存在
//            if (!isUserExists(userId)) {
//                throw new APIException(ResultCodeEnum.NOT_FOUND);
//            }
//
//            List<BusinessSearchVO> collections = interactionMapper.selectUserCollections(userId);
//            log.info("获取用户{}的收藏列表成功，共{}条记录", userId, collections.size());
//            return collections;
//
//        } catch (APIException e) {
//            log.warn("业务异常: {}", e.getMessage());
//            throw e;
//        } catch (Exception e) {
//            log.error("获取用户收藏列表失败: userId={}", userId, e);
//            throw new APIException(ResultCodeEnum.SERVER_ERROR);
//        }
//    }

    private BigDecimal calculateRating(Map<String, Object> interactionCounts) {
        try {
            int likeCount = getCount(interactionCounts.get("likeCount"));
            int collectCount = getCount(interactionCounts.get("collectCount"));

            double normalizedRating = 1 + 4 * (0.6 * likeCount / (likeCount + 10.0) + 0.4 * collectCount / (collectCount + 10.0));
            return BigDecimal.valueOf(normalizedRating).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            log.error("计算评分失败", e);
            return BigDecimal.ZERO;
        }
    }

    private int getCount(Object countObj) {
        if (countObj instanceof BigDecimal) {
            return ((BigDecimal) countObj).intValue();
        } else if (countObj instanceof Long) {
            return ((Long) countObj).intValue();
        } else if (countObj instanceof Integer) {
            return (Integer) countObj;
        }
        return 0;
    }
    @Override
    public List<BusinessSearchVO> getUserCollections(Long userId) {
        //先写一个通过用户id查询商铺id列表的方法
        List< Long> businessIds = interactionMapper.selectUserCollectionIds(userId);
        //通过商家id查询商铺信息 —— 只没有评分和销售量
        List<BusinessSearchVO> businessSearchVOS=new ArrayList<>();
        for(Long businessId:businessIds){
            BusinessVO businessVO =businessMapper.getBusinessById(businessId);
            BusinessSearchVO businessSearchVO=new BusinessSearchVO();
              //计算每一个商铺的评分和销售量

            int salesCount = businessMapper.getSalesCount(businessId);

            Integer likeCount = interactionMapper.countLikesByMerchantId(businessId);
            Integer collectCount = interactionMapper.countCollectionsByMerchantId(businessId);


            // 计算评分 (点赞权重0.6，收藏权重0.4，归一化到1-5分)
            double normalizedRating = 1 + 4 * (0.6 * likeCount / (likeCount + 10.0) + 0.4 * collectCount / (collectCount + 10.0));
            BigDecimal rating = BigDecimal.valueOf(normalizedRating).setScale(2, RoundingMode.HALF_UP);
            businessSearchVO.setScore(rating);
            businessSearchVO.setSalesCount(salesCount);
            businessSearchVOS.add(new BusinessSearchVO(businessVO.getId(),businessVO.getBusinessName(),businessVO.getBusinessImg(),businessVO.getStartPrice(),businessVO.getDeliveryPrice(),businessSearchVO.getScore(),businessSearchVO.getSalesCount()));


        }
        return businessSearchVOS;

    }

    private boolean isUserExists(Long userId) {
        Integer count = userMapper.countUserById(userId);
        return count != null && count > 0; }
    @Override
    public MerchantStatsVO getMerchantStats(Long merchantId) {
        try {
            if (merchantId == null) {
                throw new APIException(ResultCodeEnum.PARAM_NOT_MATCHED);
            }

            // 获取点赞数和收藏数
            Integer likeCount = interactionMapper.countLikesByMerchantId(merchantId);
            Integer collectCount = interactionMapper.countCollectionsByMerchantId(merchantId);
            String merchantName = interactionMapper.selectMerNameById(merchantId);
            // 计算评分 (点赞权重0.6，收藏权重0.4，归一化到1-5分)
            double normalizedRating = 1 + 4 * (0.6 * likeCount / (likeCount + 10.0) + 0.4 * collectCount / (collectCount + 10.0));
            BigDecimal rating = BigDecimal.valueOf(normalizedRating).setScale(2, RoundingMode.HALF_UP);

            MerchantStatsVO stats = new MerchantStatsVO();
            stats.setMerchantId(merchantId);
            stats.setMerchantName(merchantName);
            stats.setLikeCount(likeCount);
            stats.setCollectCount(collectCount);
            stats.setRating(rating);

            log.info("获取商家{}的统计信息成功: 点赞数={}, 收藏数={}, 评分={}",
                    merchantId, likeCount, collectCount, rating);
            return stats;

        } catch (APIException e) {
            log.warn("业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("获取商家统计信息失败: merchantId={}", merchantId, e);
            throw new APIException(ResultCodeEnum.SERVER_ERROR);
        }
    }

    @Override
    public List<MerchantStatsVO>getMerchantStatsByUserId(Long userId){
        List<Long> businessIds =businessMapper.getBusinessIdsByUserIds(userId);
        List<MerchantStatsVO> merchantStatsVOS=new ArrayList<>();
        for(Long businessId:businessIds){
            MerchantStatsVO merchantStatsVO=getMerchantStats(businessId);
            merchantStatsVOS.add(merchantStatsVO);
//            log.info("获取商铺{}的统计信息成功: 点赞数={}, 收藏数={}, 评分={}", businessId, merchantStatsVO.getLikeCount(), merchantStatsVO.getCollectCount(), merchantStatsVO.getRating());

        }
        return merchantStatsVOS;
    }

    @Override
    public MerchantInteractionVO getUserMerchantInteraction(Long userId, Long merchantId) {
        try {
            if (userId == null) {
                throw new APIException(ResultCodeEnum.PARAM_NOT_MATCHED);
            }
            if (merchantId == null) {
                throw new APIException(ResultCodeEnum.PARAM_NOT_MATCHED);
            }

            MerchantInteraction interaction = interactionMapper.selectByUserAndMerchant(userId, merchantId);

            MerchantInteractionVO vo = new MerchantInteractionVO();
            vo.setMerchantId(merchantId);
            // 如果存在互动记录，设置点赞和收藏状态
            if (interaction != null) {
                vo.setLiked(interaction.getLiked());
                vo.setCollected(interaction.getCollected());
            } else {
                // 如果不存在记录，默认都为false
                vo.setLiked(false);
                vo.setCollected(false);
            }

            log.info("获取用户{}对商家{}的互动状态成功", userId, merchantId);
            return vo;

        } catch (APIException e) {
            log.warn("业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("获取用户商家互动状态失败: userId={}, merchantId={}", userId, merchantId, e);
            throw new APIException(ResultCodeEnum.SERVER_ERROR);
        }
    }
}