package com.hmall.promotion.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AvailableCouponVO {
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
    private LocalDateTime receiveTime;
    private LocalDateTime expiredAt;

    /** 范围详情 */
    private List<String> categoryNames;
    private List<String> brandNames;
    private List<Long> shopIds;

    /** 试算结果 */
    private Integer eligibleAmount;
    private Integer discountAmount;
    private Integer payAmount;
}

