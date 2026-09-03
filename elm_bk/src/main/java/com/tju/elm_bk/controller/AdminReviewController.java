package com.tju.elm_bk.controller;

import com.tju.elm_bk.mapper.ReviewMapper;
import com.tju.elm_bk.result.HttpResult;
import com.tju.elm_bk.vo.ReviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 管理员对评价进行可恢复的展示治理，不删除原始评价。 */
@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminReviewController {
    private final ReviewMapper reviewMapper;

    @GetMapping
    public HttpResult<List<ReviewVO>> list() { return HttpResult.success(reviewMapper.listAll()); }

    @PatchMapping("/{id}/hide")
    public HttpResult<Void> hide(@PathVariable Long id) {
        if (reviewMapper.hide(id) != 1) throw new com.tju.elm_bk.exception.APIException("评价不存在或已隐藏");
        return HttpResult.success();
    }
}
