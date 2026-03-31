package com.hmall.promotion.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户领券记录表（由 MQ 消费者异步写入，避免阻塞抢券主流程）
 */
@Data
@TableName("user_coupon")
public class UserCoupon implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long couponId;

    /**
     * 使用状态
     * 1 = 未使用  2 = 已使用  3 = 已过期
     */
    private Integer status;

    /** 领取时间 */
    private LocalDateTime receiveTime;

    /** 过期时间（同优惠券活动结束时间） */
    private LocalDateTime expiredAt;

    /** 关联订单ID（使用时记录） */
    private Long orderId;

    /** 使用时间（核销时记录） */
    private LocalDateTime useTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
