package com.tju.elm_bk.service;

import com.tju.elm_bk.dto.MerchantInteractionDTO;
import com.tju.elm_bk.vo.BusinessSearchVO;
import com.tju.elm_bk.vo.MerchantInteractionVO;
import com.tju.elm_bk.vo.MerchantStatsVO;
import org.springframework.stereotype.Service;

import java.util.List;

// MerchantInteractionService.java
public interface MerchantInteractionService {
    /**
     * 更新用户对商家的互动状态（点赞/收藏）
     * @param dto 互动数据传输对象
     */
    void updateInteraction(MerchantInteractionDTO dto);

    /**
     * 获取用户的收藏列表
     * @param userId 用户ID
     * @return 收藏列表
     */
    List<BusinessSearchVO> getUserCollections(Long userId);

    /**
     * 获取商铺的统计信息
     * @param merchantId 商铺ID
     * @return 商铺统计信息
     */
    MerchantStatsVO getMerchantStats(Long merchantId);

    /**
     * 获取用户对商铺的统计信息
     * @param userId 用户ID
     * @return 商家统计信息
     */
    List<MerchantStatsVO>getMerchantStatsByUserId(Long userId);

    /**
     * 获取用户对商家的互动状态
     * @param userId 用户ID
     * @param merchantId 商家ID
     * @return 互动状态
     */
    MerchantInteractionVO getUserMerchantInteraction(Long userId, Long merchantId);
}
