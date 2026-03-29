package com.hmall.promotion.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 优惠券模板表
 *
 * <p>对应数据库表 coupon（hm-promotion 库），建表 SQL 见 db/promotion.sql
 */
@Data
@TableName("coupon")
public class Coupon implements Serializable {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 优惠券名称 */
    private String name;

    /**
     * 优惠类型
     * 1 = 满减（discountValue 单位：分）
     * 2 = 折扣（discountValue 为整数百分比，如 85 表示 85 折）
     */
    private Integer type;

    /** 优惠值（满减金额/分，或折扣百分比） */
    private Integer discountValue;

    /** 使用门槛（分），0 = 无门槛 */
    private Integer threshold;

    /** 发行总量 */
    private Integer publishCount;

    /** 剩余库存（DB 侧，Redis 侧单独维护） */
    private Integer stock;

    /** 活动开始时间 */
    private LocalDateTime beginTime;

    /** 活动结束时间（同时作为用户券过期时间） */
    private LocalDateTime endTime;

    /**
     * 状态
     * 1 = 草稿  2 = 进行中  3 = 已结束  4 = 暂停
     */
    private Integer status;

    /** 创建者用户 ID（管理员），由网关注入 user-info */
    private Long creatorId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
