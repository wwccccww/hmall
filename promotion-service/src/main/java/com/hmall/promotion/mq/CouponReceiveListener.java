package com.hmall.promotion.mq;

import com.hmall.promotion.config.PromotionRabbitMqConfig;
import com.hmall.promotion.service.IUserCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 优惠券领取 MQ 消费者。
 *
 * <p>抢券主流程只操作 Redis（Lua 脚本），通过 MQ 异步写入 user_coupon 表：
 * <ul>
 *   <li>削峰：DB 写压力平摊到消费者消费速度</li>
 *   <li>解耦：抢券接口响应不受 DB 写入延迟影响</li>
 *   <li>可靠投递：持久化队列 + 重试 + 死信；失败经 DLX 进入 DLQ 便于人工重投</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponReceiveListener {

    private final IUserCouponService userCouponService;

    @RabbitListener(queues = PromotionRabbitMqConfig.COUPON_RECEIVE_QUEUE)
    public void onCouponReceive(CouponReceiveMessage message) {
        log.info("收到领券消息，messageId={}, userId={}, couponId={}", message.getMessageId(), message.getUserId(), message.getCouponId());
        userCouponService.saveFromMessage(message);
    }
}
