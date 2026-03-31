package com.hmall.promotion.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class CouponAvailableRequest {
    @NotEmpty(message = "items 不能为空")
    private List<CouponItemDTO> items;
}

