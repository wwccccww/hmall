package com.hmall.promotion.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.promotion.domain.po.UserCoupon;
import com.hmall.promotion.mapper.UserCouponMapper;
import com.hmall.promotion.mq.CouponReceiveMessage;
import com.hmall.promotion.service.IUserCouponService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class UserCouponServiceImpl extends ServiceImpl<UserCouponMapper, UserCoupon>
        implements IUserCouponService {

    @Override
    public void saveFromMessage(CouponReceiveMessage msg) {
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(msg.getUserId());
        userCoupon.setCouponId(msg.getCouponId());
        userCoupon.setStatus(1);                   // 未使用
        userCoupon.setReceiveTime(LocalDateTime.now());
        userCoupon.setExpiredAt(msg.getExpiredAt());
        save(userCoupon);
        log.info("领券记录已落库，userId={}, couponId={}", msg.getUserId(), msg.getCouponId());
    }
}
