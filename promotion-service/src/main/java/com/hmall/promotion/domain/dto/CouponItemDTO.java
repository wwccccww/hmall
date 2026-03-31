package com.hmall.promotion.domain.dto;

import lombok.Data;

@Data
public class CouponItemDTO {
    private Long itemId;
    /** 单价（分） */
    private Integer price;
    /** 数量 */
    private Integer num;
    /** 类目名称（与 item.category 一致） */
    private String category;

    /** 品牌名称（与 item.brand 一致） */
    private String brand;

    /** 商家ID（若业务存在商家体系） */
    private Long shopId;
}

