package com.hmall.promotion.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 秒杀抢券：服务侧按「用户 + 券」令牌桶（Redis Lua）。
 */
@Data
@ConfigurationProperties(prefix = "hm.seckill.receive-rate-limit")
public class SeckillReceiveRateLimitProperties {

    /** 是否启用服务侧令牌桶 */
    private boolean enabled = true;

    /** 每秒补充令牌数 */
    private double replenishPerSecond = 10;

    /** 桶容量（突发上限） */
    private int burst = 20;
}
