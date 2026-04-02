package com.hmall.ai.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 调用阿里云百炼「智能体/工作流」应用的 OpenAI 兼容 Responses API。
 * 应用在控制台绑定知识库后，由百炼侧完成检索增强（内置 RAG）。
 *
 * @see <a href="https://help.aliyun.com/zh/model-studio/responses-api-reference">Responses API</a>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BailianResponsesClient {

    private final LlmProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean enabled() {
        String id = props.getBailianAppId();
        return id != null && !id.isBlank();
    }

    /**
     * 同步调用，解析 output[].content[] 中 type=output_text 的文本。
     */
    public String complete(String userInput) {
        if (!enabled()) {
            return "";
        }
        WebClient client = WebClient.builder()
                .baseUrl(bailianOrigin())
                .defaultHeader("Authorization", "Bearer " + nullToEmpty(props.getApiKey()))
                .build();
        Map<String, Object> body = new HashMap<>();
        body.put("input", userInput);
        body.put("stream", false);
        String json = client.post()
                .uri(responsesPath())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(props.getTimeoutMs()))
                .block();
        return extractOutputText(json);
    }

    private String bailianOrigin() {
        String base = props.getBailianEndpointBase();
        if (base == null || base.isBlank()) {
            base = "https://dashscope.aliyuncs.com";
        }
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    private String responsesPath() {
        String appId = props.getBailianAppId().trim();
        return "/api/v2/apps/agent/" + appId + "/compatible-mode/v1/responses";
    }

    private String extractOutputText(String json) {
        if (json == null || json.isBlank()) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode output = root.path("output");
            if (!output.isArray()) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            for (JsonNode item : output) {
                JsonNode content = item.path("content");
                if (!content.isArray()) {
                    continue;
                }
                for (JsonNode part : content) {
                    if ("output_text".equals(part.path("type").asText())) {
                        sb.append(part.path("text").asText(""));
                    }
                }
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("parse Bailian response failed: {}", e.getMessage());
            return "";
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
