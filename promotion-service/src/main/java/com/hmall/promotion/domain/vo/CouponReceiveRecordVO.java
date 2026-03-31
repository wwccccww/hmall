package com.hmall.promotion.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CouponReceiveRecordVO {
    private Long userId;
    private Integer status;
    private LocalDateTime receiveTime;
    private LocalDateTime expiredAt;
}

