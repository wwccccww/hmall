package com.hmall.api.dto.promotion;

import lombok.Data;

@Data
public class CouponPreviewVO {
    private Long couponId;
    private Integer totalAmount;
    private Integer eligibleAmount;
    private Integer discountAmount;
    private Integer payAmount;
}

