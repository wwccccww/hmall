package com.hmall.api.dto.promotion;

import lombok.Data;

import java.util.List;

@Data
public class CouponPreviewRequest {
    private Long couponId;
    private List<CouponItemDTO> items;
}

