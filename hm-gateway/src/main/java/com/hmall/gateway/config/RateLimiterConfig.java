package com.hmall.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 秒杀抢券网关限流：按路径中的券 ID 聚合（同一活动共用一个 Redis 桶）。
 */
@Configuration
public class RateLimiterConfig {

    private static final Pattern COUPON_RECEIVE_PATH = Pattern.compile("^/coupons/(\\d+)/receive$");// 定义抢券路径的正则表达式

    @Bean
    public KeyResolver couponReceiveKeyResolver() {
        return exchange -> {
            String path = exchange.getRequest().getURI().getPath(); // 获取请求路径
            Matcher m = COUPON_RECEIVE_PATH.matcher(path); // 匹配路径
            if (m.matches()) { // 如果匹配成功
                return Mono.just("couponReceive:" + m.group(1)); // 返回令牌桶的键
            }
            return Mono.just("couponReceive:unknown"); // 返回默认键
        };
    }
}
