package com.hmall.promotion.mapper;

import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmall.promotion.domain.po.Coupon;

public interface CouponMapper extends BaseMapper<Coupon> {

    @Update("UPDATE coupon SET stock = stock - 1 WHERE id = #{couponId} AND stock > 0")
    boolean decrementStock(Long couponId);
}
