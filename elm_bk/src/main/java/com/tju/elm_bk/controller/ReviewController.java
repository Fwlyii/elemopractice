package com.tju.elm_bk.controller;

import com.tju.elm_bk.dto.ReviewCreateDTO;
import com.tju.elm_bk.result.HttpResult;
import com.tju.elm_bk.service.ReviewService;
import com.tju.elm_bk.vo.ReviewVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping
    @PreAuthorize("hasAuthority('USER')")
    public HttpResult<ReviewVO> create(@Valid @RequestBody ReviewCreateDTO dto) { return HttpResult.success(reviewService.create(dto)); }
    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAuthority('USER')")
    public HttpResult<ReviewVO> getByOrder(@PathVariable Long orderId) { return HttpResult.success(reviewService.getByOrder(orderId)); }
    @GetMapping("/business/{businessId}")
    public HttpResult<List<ReviewVO>> listByBusiness(@PathVariable Long businessId) { return HttpResult.success(reviewService.listByBusiness(businessId)); }
    @PutMapping("/{reviewId}/reply")
    @PreAuthorize("hasAuthority('BUSINESS')")
    public HttpResult<Void> reply(@PathVariable Long reviewId, @RequestBody java.util.Map<String,String> body) { reviewService.reply(reviewId, body.get("reply")); return HttpResult.success(); }
}
