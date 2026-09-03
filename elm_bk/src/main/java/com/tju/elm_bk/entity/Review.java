package com.tju.elm_bk.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Review {
    private Long id;
    private Long orderId;
    private Long customerId;
    private Long businessId;
    private Integer rating;
    private String content;
    private String images;
    private String merchantReply;
    private LocalDateTime replyTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Boolean hidden;
}
