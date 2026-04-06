package com.hmall.ai;

import com.hmall.api.config.DefaultFeignConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import com.hmall.ai.config.AiDownstreamProperties;
import com.hmall.ai.knowledge.EmbeddingProperties;
import com.hmall.ai.knowledge.KnowledgeProperties;
import com.hmall.ai.llm.LlmProperties;

@EnableFeignClients(defaultConfiguration = DefaultFeignConfig.class)
@EnableConfigurationProperties({
    LlmProperties.class,
    KnowledgeProperties.class,
    EmbeddingProperties.class,
    AiDownstreamProperties.class
})
@SpringBootApplication
public class AiAssistantApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiAssistantApplication.class, args);
    }
}

