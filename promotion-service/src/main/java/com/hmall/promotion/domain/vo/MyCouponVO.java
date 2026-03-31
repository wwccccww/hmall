package com.hmall.promotion.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MyCouponVO {
    private Long id;
    private String name;
    private Integer type;
    private Integer discountValue;
    private Integer threshold;
    private Integer stock;
    private LocalDateTime beginTime;
    private LocalDateTime endTime;
    private Integer status;
    private Long creatorId;
    private Integer scopeType;

    /** user_coupon 字段 */
    private Integer userCouponStatus;
    private LocalDateTime receiveTime;
    private LocalDateTime expiredAt;

    /** 便于前端展示范围详情（字符串匹配类） */
    private List<String> categoryNames;
    private List<String> brandNames;
    private List<Long> shopIds;
}

