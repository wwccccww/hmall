package com.hmall.promotion.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.promotion.domain.po.Coupon;
import com.hmall.promotion.domain.po.UserCoupon;
import com.hmall.promotion.mapper.CouponMapper;
import com.hmall.promotion.mapper.UserCouponMapper;
import com.hmall.promotion.mq.CouponReceiveMessage;
import com.hmall.promotion.service.ICouponService;
import com.hmall.promotion.service.IUserCouponService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCouponServiceImpl extends ServiceImpl<UserCouponMapper, UserCoupon>
        implements IUserCouponService {


    private final CouponMapper couponMapper;

    @Override
    @Transactional
    public void saveFromMessage(CouponReceiveMessage msg) {
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(msg.getUserId());
        userCoupon.setCouponId(msg.getCouponId());
        userCoupon.setStatus(1);                   // 未使用
        userCoupon.setReceiveTime(LocalDateTime.now());
        userCoupon.setExpiredAt(msg.getExpiredAt());
        try {
            save(userCoupon);
            log.info("领券记录已落库，userId={}, couponId={}", msg.getUserId(), msg.getCouponId());
        } catch (DuplicateKeyException e) {
            // 幂等：MQ 可能重复投递，DB 唯一约束兜底后直接忽略即可
            log.warn("领券记录已存在，忽略重复落库，userId={}, couponId={}", msg.getUserId(), msg.getCouponId());
        }

        // 正确做法：使用 MyBatis-Plus 的 updateById 或者自定义 SQL 扣减库存
        // 假设 couponMapper 有一个自定义方法 decrementStock(Long couponId)
        boolean update = couponMapper.decrementStock(msg.getCouponId());
        
        if (!update) {
            log.error("扣减优惠券库存失败，userId={}, couponId={}", msg.getUserId(), msg.getCouponId());
        }
    }
}
