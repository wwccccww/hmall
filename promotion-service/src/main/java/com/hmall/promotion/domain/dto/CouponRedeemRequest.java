package com.hmall.promotion.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class CouponRedeemRequest {
    @NotNull(message = "couponId 不能为空")
    private Long couponId;

    @NotNull(message = "orderId 不能为空")
    private Long orderId;

    @NotEmpty(message = "items 不能为空")
    private List<CouponItemDTO> items;

    @NotNull(message = "userId 不能为空")
    private Long userId;
}

