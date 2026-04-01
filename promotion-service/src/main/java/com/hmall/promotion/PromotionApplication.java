package com.hmall.promotion;

import com.hmall.promotion.config.SeckillReceiveRateLimitProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan("com.hmall.promotion.mapper")
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(SeckillReceiveRateLimitProperties.class)
public class PromotionApplication {
    public static void main(String[] args) {
        SpringApplication.run(PromotionApplication.class, args);
    }
}
