package com.tju.elm_bk.service;

import com.tju.elm_bk.dto.ReviewCreateDTO;
import com.tju.elm_bk.vo.ReviewVO;

import java.util.List;

public interface ReviewService {
    ReviewVO create(ReviewCreateDTO dto);
    ReviewVO getByOrder(Long orderId);
    List<ReviewVO> listByBusiness(Long businessId);
    void reply(Long reviewId, String reply);
}
