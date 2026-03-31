package com.hmall.promotion.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("coupon_shop")
public class CouponShop implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long couponId;
    private Long shopId;
    private LocalDateTime createTime;
}

