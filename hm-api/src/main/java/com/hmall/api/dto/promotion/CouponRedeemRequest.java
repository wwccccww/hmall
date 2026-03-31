package com.hmall.api.dto.promotion;

import lombok.Data;

import java.util.List;

@Data
public class CouponRedeemRequest {
    private Long couponId;
    private Long orderId;
    /** 下单/支付成功后的核销用户，用于不依赖网关 user-info 注入 */
    private Long userId;
    private List<CouponItemDTO> items;
}

