package com.hmall.promotion.mq;

import com.hmall.promotion.service.IUserCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 优惠券领取 MQ 消费者。
 *
 * <p>抢券主流程只操作 Redis（Lua 脚本），通过 MQ 异步写入 user_coupon 表：
 * <ul>
 *   <li>削峰：DB 写压力平摊到消费者消费速度</li>
 *   <li>解耦：抢券接口响应不受 DB 写入延迟影响</li>
 *   <li>可靠投递：RabbitMQ 持久化队列 + 手动 ACK（Spring 默认 AUTO_ACK）</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponReceiveListener {

    private final IUserCouponService userCouponService;

    @RabbitListener(bindings = @QueueBinding(
            value    = @Queue(value = "promotion.coupon.receive.queue", durable = "true"),
            exchange = @Exchange(name = "promotion.topic", type = "topic"),
            key      = "coupon.receive"
    ))
    public void onCouponReceive(CouponReceiveMessage message) {
        log.info("收到领券消息，userId={}, couponId={}", message.getUserId(), message.getCouponId());
        userCouponService.saveFromMessage(message);
    }
}
