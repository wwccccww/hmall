package com.hmall.promotion.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户抢券成功后发往 MQ 的消息体（异步落库 user_coupon 表）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CouponReceiveMessage {

    /** 消息幂等键（发送端生成 UUID） */
    private String messageId;

    private Long userId;

    private Long couponId;

    /** 过期时间（与优惠券活动结束时间一致） */
    private LocalDateTime expiredAt;
}
