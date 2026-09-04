// MerchantInteractionServiceImpl.java
package com.tju.elm_bk.service.impl;

import com.tju.elm_bk.entity.MerchantInteraction;
import com.tju.elm_bk.entity.User;
import com.tju.elm_bk.mapper.BusinessMapper;
import com.tju.elm_bk.mapper.MerchantInteractionMapper;
import com.tju.elm_bk.service.CurrentUserService;
import com.tju.elm_bk.service.MerchantInteractionService;
import com.tju.elm_bk.vo.BusinessSearchVO;
import com.tju.elm_bk.vo.MerchantInteractionVO;
import com.tju.elm_bk.vo.MerchantStatsVO;
import com.tju.elm_bk.dto.MerchantInteractionDTO;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.result.ResultCodeEnum;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class MerchantInteractionServiceImpl implements MerchantInteractionService {

    private final MerchantInteractionMapper interactionMapper;
    private final BusinessMapper businessMapper;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public void updateInteraction(MerchantInteractionDTO dto) {
        try {
            // 参数验证
            if (dto.getMerchantId() == null) {
                throw new APIException(ResultCodeEnum.PARAM_NOT_MATCHED);
            }

            User currentUser = currentUser();
            Long operatorId = currentUser.getId();

            // 查询现有记录
            MerchantInteraction interaction = interactionMapper.selectByUserAndMerchant(
                    operatorId, dto.getMerchantId());

            if (interaction == null) {
                // 创建新记录
                interaction = new MerchantInteraction();
                interaction.setUserId(operatorId);
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

            log.info("用户{}对商家{}的互动状态更新成功", operatorId, dto.getMerchantId());

        } catch (APIException e) {
            log.warn("业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("更新用户商家互动失败: merchantId={}", dto.getMerchantId(), e);
            throw new APIException(ResultCodeEnum.SERVER_ERROR);
        }
    }

    @Override
    public List<BusinessSearchVO> getUserCollections(Long userId) {
        User currentUser = currentUser();
        Long targetUserId = userId == null ? currentUser.getId() : userId;
        if (!currentUserService.isAdmin(currentUser) && !Objects.equals(targetUserId, currentUser.getId())) {
            throw new APIException(ResultCodeEnum.USER_UNMATCHED);
        }
        return businessMapper.selectCollectedBusinesses(targetUserId);
    }

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
            BusinessSearchVO summary = businessMapper.getBusinessSummaryById(merchantId);
            if (summary == null) throw new APIException(ResultCodeEnum.BUSINESS_MISSED);
            BigDecimal rating = summary.getScore();

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
    public List<MerchantStatsVO> getMerchantStatsByUserId(Long userId) {
        User currentUser = currentUser();
        Long targetUserId = userId == null ? currentUser.getId() : userId;
        if (!currentUserService.isAdmin(currentUser) && !Objects.equals(targetUserId, currentUser.getId())) {
            throw new APIException(ResultCodeEnum.USER_UNMATCHED);
        }

        List<Long> businessIds = businessMapper.getBusinessIdsByUserIds(targetUserId);
        List<MerchantStatsVO> merchantStatsVOS = new ArrayList<>();
        for (Long businessId : businessIds) {
            MerchantStatsVO merchantStatsVO = getMerchantStats(businessId);
            merchantStatsVOS.add(merchantStatsVO);
        }
        return merchantStatsVOS;
    }

    @Override
    public MerchantInteractionVO getUserMerchantInteraction(Long userId, Long merchantId) {
        try {
            if (merchantId == null) {
                throw new APIException(ResultCodeEnum.PARAM_NOT_MATCHED);
            }

            User currentUser = currentUser();
            Long targetUserId = userId == null ? currentUser.getId() : userId;
            if (!currentUserService.isAdmin(currentUser) && !Objects.equals(targetUserId, currentUser.getId())) {
                throw new APIException(ResultCodeEnum.USER_UNMATCHED);
            }

            MerchantInteraction interaction = interactionMapper.selectByUserAndMerchant(targetUserId, merchantId);

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

            log.info("获取用户{}对商家{}的互动状态成功", targetUserId, merchantId);
            return vo;

        } catch (APIException e) {
            log.warn("业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("获取用户商家互动状态失败: merchantId={}", merchantId, e);
            throw new APIException(ResultCodeEnum.SERVER_ERROR);
        }
    }

    private User currentUser() {
        return currentUserService.requireUser();
    }
}
