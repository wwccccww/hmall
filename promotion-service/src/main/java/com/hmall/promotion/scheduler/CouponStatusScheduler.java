package com.hmall.promotion.scheduler;

import com.hmall.promotion.service.ICouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 优惠券状态自动维护定时任务。
 *
 * <p>每分钟扫描一次，将活动已结束（endTime < now）但状态仍为进行中（2）的券
 * 自动置为已结束（3），保证数据库与实际时间一致，管理端列表状态实时准确。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponStatusScheduler {

    private final ICouponService couponService;

    @Scheduled(fixedDelay = 60_000)
    public void autoExpire() {
        try {
            couponService.autoExpireCoupons();
        } catch (Exception e) {
            log.error("自动过期优惠券任务异常", e);
        }
    }
}
