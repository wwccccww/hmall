package com.hmall.trade.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 支付成功队列 + 死信（与 pay-service 发送的 exchange / routingKey 一致）。
 *
 * <p>使用 {@code .v2} 队列名与路由键，避免与历史上无 DLX 的 {@code trade.pay.success.queue} 在 Broker 里参数冲突。
 */
@Configuration
public class TradeRabbitMqConfig {

    public static final String PAY_EXCHANGE = "pay.direct";
    public static final String PAY_SUCCESS_RK = "pay.success.v2";
    public static final String PAY_QUEUE = "trade.pay.success.queue.v2";

    public static final String PAY_DLX = "trade.pay.dlx";
    public static final String PAY_DLQ = "trade.pay.success.dlq.v2";
    public static final String PAY_DLQ_RK = "trade.pay.success.dead.v2";

    @Bean
    public DirectExchange payDirectExchange() {
        return new DirectExchange(PAY_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange payDlxExchange() {
        return new DirectExchange(PAY_DLX, true, false);
    }

    @Bean
    public Queue paySuccessDeadLetterQueue() {
        return QueueBuilder.durable(PAY_DLQ).build();
    }

    @Bean
    public Binding payDlqBinding() {
        return BindingBuilder.bind(paySuccessDeadLetterQueue()).to(payDlxExchange()).with(PAY_DLQ_RK);
    }

    @Bean
    public Queue paySuccessQueue() {
        return QueueBuilder.durable(PAY_QUEUE)
                .withArgument("x-dead-letter-exchange", PAY_DLX)
                .withArgument("x-dead-letter-routing-key", PAY_DLQ_RK)
                .build();
    }

    @Bean
    public Binding paySuccessBinding() {
        return BindingBuilder.bind(paySuccessQueue()).to(payDirectExchange()).with(PAY_SUCCESS_RK);
    }
}
