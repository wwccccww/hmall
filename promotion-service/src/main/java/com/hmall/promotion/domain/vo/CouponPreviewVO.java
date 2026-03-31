package com.hmall.promotion.domain.vo;

import lombok.Data;

@Data
public class CouponPreviewVO {
    private Long couponId;
    /** 订单原始总金额（分） */
    private Integer totalAmount;
    /** 满足规则可参与优惠的金额（分） */
    private Integer eligibleAmount;
    /** 优惠金额（分） */
    private Integer discountAmount;
    /** 应付金额（分） */
    private Integer payAmount;
}

