package com.hmall.promotion.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 抢券异步队列 + 死信（与 {@link com.hmall.promotion.service.impl.CouponServiceImpl} 中 exchange / routingKey 一致）。
 *
 * <p>使用 {@code .v2} 队列名与路由键：避免与历史上无 DLX 参数的同名队列在 Broker 里冲突（RabbitMQ 不允许改队列参数）。
 * 旧队列 {@code promotion.coupon.receive.queue} / 路由 {@code coupon.receive} 可在控制台删除以免残留。
 */
@Configuration
public class PromotionRabbitMqConfig {

    public static final String PROMOTION_TOPIC = "promotion.topic";
    public static final String COUPON_RECEIVE_RK = "coupon.receive.v2";
    public static final String COUPON_RECEIVE_QUEUE = "promotion.coupon.receive.queue.v2";

    public static final String PROMOTION_DLX = "promotion.coupon.dlx";
    public static final String COUPON_RECEIVE_DLQ = "promotion.coupon.receive.dlq.v2";
    public static final String COUPON_RECEIVE_DLQ_RK = "promotion.coupon.receive.dead.v2";

    @Bean
    public TopicExchange promotionTopicExchange() {
        return new TopicExchange(PROMOTION_TOPIC, true, false);
    }

    @Bean
    public TopicExchange promotionDlxExchange() {
        return new TopicExchange(PROMOTION_DLX, true, false);
    }

    @Bean
    public Queue couponReceiveDeadLetterQueue() {
        return QueueBuilder.durable(COUPON_RECEIVE_DLQ).build();
    }

    @Bean
    public Binding couponReceiveDlqBinding() {
        return BindingBuilder.bind(couponReceiveDeadLetterQueue()).to(promotionDlxExchange()).with(COUPON_RECEIVE_DLQ_RK);
    }

    @Bean
    public Queue couponReceiveQueue() {
        return QueueBuilder.durable(COUPON_RECEIVE_QUEUE)
                .withArgument("x-dead-letter-exchange", PROMOTION_DLX)
                .withArgument("x-dead-letter-routing-key", COUPON_RECEIVE_DLQ_RK)
                .build();
    }

    @Bean
    public Binding couponReceiveBinding() {
        return BindingBuilder.bind(couponReceiveQueue()).to(promotionTopicExchange()).with(COUPON_RECEIVE_RK);
    }
}
