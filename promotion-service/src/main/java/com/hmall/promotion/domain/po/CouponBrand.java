package com.hmall.promotion.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("coupon_brand")
public class CouponBrand implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long couponId;
    private String brandName;
    private LocalDateTime createTime;
}

