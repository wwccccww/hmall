package com.hmall.ai.llm;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "hm.ai.llm")
public class LlmProperties {
    /**
     * OpenAI 兼容 API base url，例如 https://api.openai.com
     */
    private String baseUrl = "https://api.openai.com";
    private String apiKey;
    private String model = "gpt-4o-mini";
    private int timeoutMs = 15000;
}

