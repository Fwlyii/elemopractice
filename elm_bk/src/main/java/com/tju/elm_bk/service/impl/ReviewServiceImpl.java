package com.tju.elm_bk.service.impl;

import com.tju.elm_bk.constant.OrderStatus;
import com.tju.elm_bk.dto.ReviewCreateDTO;
import com.tju.elm_bk.entity.Business;
import com.tju.elm_bk.entity.Order;
import com.tju.elm_bk.entity.Review;
import com.tju.elm_bk.entity.User;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.mapper.BusinessMapper;
import com.tju.elm_bk.mapper.OrdersMapper;
import com.tju.elm_bk.mapper.ReviewMapper;
import com.tju.elm_bk.service.CurrentUserService;
import com.tju.elm_bk.service.ReviewService;
import com.tju.elm_bk.vo.ReviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {
    private final ReviewMapper reviewMapper;
    private final OrdersMapper ordersMapper;
    private final BusinessMapper businessMapper;
    private final CurrentUserService currentUserService;

    private User currentUser() {
        return currentUserService.requireUser();
    }

    @Override
    @Transactional
    public ReviewVO create(ReviewCreateDTO dto) {
        User user = currentUser();
        Order order = ordersMapper.getOrderById(dto.getOrderId());
        if (order == null || !Objects.equals(order.getCustomerId(), user.getId())) throw new APIException("只能评价自己的订单");
        if (!Objects.equals(order.getOrderState(), OrderStatus.COMPLETED.getCode())) throw new APIException("订单完成后才能评价");
        if (reviewMapper.selectByOrderId(dto.getOrderId()) != null) throw new APIException("该订单已经评价过了");
        Review review = new Review();
        review.setOrderId(order.getId()); review.setCustomerId(user.getId()); review.setBusinessId(order.getBusinessId());
        review.setRating(dto.getRating()); review.setContent(dto.getContent() == null ? "" : dto.getContent().trim()); review.setImages(dto.getImages());
        reviewMapper.insert(review);
        return reviewMapper.selectByOrderId(order.getId());
    }

    @Override public ReviewVO getByOrder(Long orderId) {
        User user = currentUser(); Order order = ordersMapper.getOrderById(orderId);
        if (order == null || !Objects.equals(order.getCustomerId(), user.getId())) throw new APIException("无权查看该评价");
        return reviewMapper.selectByOrderId(orderId);
    }

    @Override public List<ReviewVO> listByBusiness(Long businessId) { return reviewMapper.listByBusiness(businessId); }

    @Override @Transactional
    public void reply(Long reviewId, String reply) {
        User user = currentUser();
        if (reply == null || reply.trim().isEmpty() || reply.length() > 500) throw new APIException("回复内容需为1-500字");
        ReviewVO review = reviewMapper.selectById(reviewId);
        if (review == null) throw new APIException("评价不存在");
        Business business = businessMapper.selectBusinessById(review.getBusinessId());
        if (business == null || !Objects.equals(business.getUserId(), user.getId())) throw new APIException("只能回复自己店铺的评价");
        if (reviewMapper.updateReply(review.getId(), reply.trim()) != 1) throw new APIException("评价已回复，请勿重复提交");
    }
}
