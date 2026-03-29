package com.hmall.promotion.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmall.promotion.domain.po.UserCoupon;
import com.hmall.promotion.mq.CouponReceiveMessage;

public interface IUserCouponService extends IService<UserCoupon> {

    /** 根据 MQ 消息落库 user_coupon 记录 */
    void saveFromMessage(CouponReceiveMessage msg);
}
