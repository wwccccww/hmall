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

    /**
     * 百炼控制台「应用」ID。非空时，通用导购优先走智能体 Responses API（可在应用内绑定知识库，使用内置 RAG）。
     */
    private String bailianAppId;

    /**
     * 百炼网关域名（北京默认）。新加坡等国际域可改为对应 endpoint。
     */
    private String bailianEndpointBase = "https://dashscope.aliyuncs.com";
}

