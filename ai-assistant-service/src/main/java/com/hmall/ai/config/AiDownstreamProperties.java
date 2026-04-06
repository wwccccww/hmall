package com.hmall.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 导购工具调用用到的下游服务根地址（不带路径尾斜杠）。
 */
@Data
@ConfigurationProperties(prefix = "hm.ai.downstream")
public class AiDownstreamProperties {

    /** user-service（/users/me、/addresses） */
    private String userServiceUrl = "http://localhost:8084";

    /** trade-service（/orders/{id}） */
    private String tradeServiceUrl = "http://localhost:8085";

    /** promotion-service（/coupons/my） */
    private String promotionServiceUrl = "http://localhost:8087";

    public String userBase() {
        return trimSlash(userServiceUrl);
    }

    public String tradeBase() {
        return trimSlash(tradeServiceUrl);
    }

    public String promotionBase() {
        return trimSlash(promotionServiceUrl);
    }

    private static String trimSlash(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        String s = url.trim();
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
