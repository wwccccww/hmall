package com.hmall.api.dto.promotion;

import lombok.Data;

@Data
public class CouponItemDTO {
    private Long itemId;
    private Integer price;
    private Integer num;
    private String category;
    private String brand;
    private Long shopId;
}

