package com.hmall.pay.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 与 trade-service 中声明的 exchange 同名同类型，保证 pay 先于 trade 启动时发消息不会 404。
 */
@Configuration
public class PayRabbitMqTopologyConfig {

    @Bean
    public DirectExchange payDirectExchange() {
        return new DirectExchange("pay.direct", true, false);
    }
}
