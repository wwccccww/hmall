package com.hmall.common.config;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
@Configuration
public class MqConfig {
    // 配置序列化
    @Bean
    public MessageConverter messageConverter() {
        // 1. 实例化 ObjectMapper
        ObjectMapper om = new ObjectMapper();
        // 2. 注册 JSR310 模块（处理 LocalDateTime）
        om.registerModule(new JavaTimeModule());
        // 3. 设置转换器使用这个定制的 ObjectMapper
        return new Jackson2JsonMessageConverter(om);
    }
}
