package com.tju.elm_bk.mapper;

import com.tju.elm_bk.entity.Review;
import com.tju.elm_bk.vo.ReviewVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReviewMapper {
    ReviewVO selectByOrderId(@Param("orderId") Long orderId);
    ReviewVO selectById(@Param("id") Long id);
    void insert(Review review);
    int updateReply(@Param("id") Long id, @Param("reply") String reply);
    List<ReviewVO> listByBusiness(@Param("businessId") Long businessId);
    List<ReviewVO> listAll();
    int hide(@Param("id") Long id);
}
