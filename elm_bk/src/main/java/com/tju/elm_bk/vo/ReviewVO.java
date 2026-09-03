package com.tju.elm_bk.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewVO {
    private Long id;
    private Long orderId;
    private Long customerId;
    private String customerName;
    private Long businessId;
    private String businessName;
    private Integer rating;
    private String content;
    private String images;
    private String merchantReply;
    private LocalDateTime replyTime;
    private LocalDateTime createTime;
    private Boolean hidden;
}
