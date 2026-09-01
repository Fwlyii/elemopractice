package com.tju.elm_bk.controller;

//点赞收藏的接口

import com.tju.elm_bk.dto.MerchantInteractionDTO;
import com.tju.elm_bk.service.MerchantInteractionService;
import com.tju.elm_bk.vo.BusinessSearchVO;
import com.tju.elm_bk.vo.MerchantInteractionVO;
import com.tju.elm_bk.vo.MerchantStatsVO;
import com.tju.elm_bk.result.HttpResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/merchant/interaction")
@Validated
@Tag(name="商铺点赞收藏等一系列")
public class MerchantInteractionController {

    @Autowired
    private MerchantInteractionService interactionService;

    @PostMapping("/update")
    @Operation(summary = "更新商铺点赞收藏等信息")
    public HttpResult<?> updateInteraction(@Valid @RequestBody MerchantInteractionDTO dto) {
        interactionService.updateInteraction(dto);
        return HttpResult.success("操作成功");
    }

    @GetMapping("/collections/{userId}")
    @Operation(summary = "获取某用户收藏列表")
    public HttpResult<List<BusinessSearchVO>> getUserCollections(@PathVariable Long userId) {
        List<BusinessSearchVO> collections = interactionService.getUserCollections(userId);
        return HttpResult.success(collections);
    }

    @GetMapping("/stats/{merchantId}")
    @Operation(summary = "获取某商铺点赞收藏总数")
    public HttpResult<MerchantStatsVO> getMerchantStats(@PathVariable Long merchantId) {
        MerchantStatsVO stats = interactionService.getMerchantStats(merchantId);
        return HttpResult.success(stats);
    }
    @GetMapping("/statsByUserId/{userId}")
    @Operation(summary = "获取某商家下的所有商铺的点赞收藏总数")
    public HttpResult<List<MerchantStatsVO>> getMerchantStatsByUserId(@PathVariable Long userId) {
        List<MerchantStatsVO> stats = interactionService.getMerchantStatsByUserId(userId);
        return HttpResult.success(stats);
    }

    @GetMapping("/status")
    @Operation(summary = "获取用户商铺互动状态")
    public HttpResult<MerchantInteractionVO> getUserMerchantInteraction(
            @RequestParam Long userId,
            @RequestParam Long merchantId) {
        MerchantInteractionVO status = interactionService.getUserMerchantInteraction(userId, merchantId);
        return HttpResult.success(status);
    }
}
