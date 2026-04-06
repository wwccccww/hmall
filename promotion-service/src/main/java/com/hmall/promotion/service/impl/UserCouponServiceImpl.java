package com.hmall.promotion.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.common.exception.BizIllegalException;
import com.hmall.promotion.domain.po.Coupon;
import com.hmall.promotion.domain.po.MqIdempotentConsume;
import com.hmall.promotion.domain.po.UserCoupon;
import com.hmall.promotion.mapper.CouponMapper;
import com.hmall.promotion.mapper.MqIdempotentConsumeMapper;
import com.hmall.promotion.mapper.UserCouponMapper;
import com.hmall.promotion.mq.CouponReceiveMessage;
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

    private static final String BIZ_COUPON_RECEIVE = "coupon_receive";

    private final CouponMapper couponMapper;
    private final MqIdempotentConsumeMapper mqIdempotentConsumeMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveFromMessage(CouponReceiveMessage msg) {
        try {
            mqIdempotentConsumeMapper.insert(new MqIdempotentConsume(
                    null, BIZ_COUPON_RECEIVE, msg.getMessageId(), LocalDateTime.now()));
        } catch (DuplicateKeyException e) {
            log.warn("MQ 重复消费，已幂等跳过 messageId={}", msg.getMessageId());
            return;
        }

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(msg.getUserId());
        userCoupon.setCouponId(msg.getCouponId());
        userCoupon.setStatus(1);
        userCoupon.setReceiveTime(LocalDateTime.now());
        userCoupon.setExpiredAt(msg.getExpiredAt());
        try {
            save(userCoupon);
            log.info("领券记录已落库，userId={}, couponId={}", msg.getUserId(), msg.getCouponId());
        } catch (DuplicateKeyException e) {
            // 同一用户券已存在：不再扣 DB 库存，避免重复扣减（与 Lua 已扣 Redis 对齐依赖业务幂等）
            log.warn("领券记录已存在，跳过库存扣减 userId={}, couponId={}", msg.getUserId(), msg.getCouponId());
            return;
        }

        boolean updated = couponMapper.decrementStock(msg.getCouponId());
        if (!updated) {
            throw new BizIllegalException("异步扣减优惠券库存失败，将重试或进入死信队列");
        }
    }
}
