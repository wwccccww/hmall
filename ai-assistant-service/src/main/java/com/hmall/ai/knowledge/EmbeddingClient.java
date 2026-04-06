package com.hmall.ai.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmall.ai.llm.LlmProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容 Embeddings：POST {baseUrl}/v1/embeddings
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingClient {

    private final EmbeddingProperties embeddingProperties;
    private final LlmProperties llmProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 对多条文本嵌入，顺序与输入一致。
     */
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        String key = resolveApiKey();
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("hm.ai.embedding.api-key 与 hm.ai.llm.api-key 均未配置，无法请求 Embeddings");
        }
        String base = trimTrailingSlash(embeddingProperties.getBaseUrl());
        WebClient client = WebClient.builder()
                .baseUrl(base)
                .defaultHeader("Authorization", "Bearer " + key)
                .build();
        Map<String, Object> body = new HashMap<>();
        body.put("model", embeddingProperties.getModel());
        body.put("input", texts);
        String json = client.post()
                .uri("/v1/embeddings")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(embeddingProperties.getTimeoutMs()))
                .block();
        if (json == null || json.isBlank()) {
            return List.of();
        }
        return parseEmbeddings(json, texts.size());
    }

    private List<float[]> parseEmbeddings(String json, int expectedMin) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode data = root.path("data");
            if (!data.isArray()) {
                log.warn("embeddings response missing data[]");
                return List.of();
            }
            List<float[]> rows = new ArrayList<>();
            for (JsonNode item : data) {
                JsonNode emb = item.path("embedding");
                if (!emb.isArray()) {
                    continue;
                }
                float[] vec = new float[emb.size()];
                int i = 0;
                for (JsonNode n : emb) {
                    vec[i++] = (float) n.asDouble();
                }
                rows.add(vec);
            }
            if (rows.size() < expectedMin) {
                log.warn("embeddings count {} < input {}", rows.size(), expectedMin);
            }
            return rows;
        } catch (Exception e) {
            throw new RuntimeException("parse embeddings failed: " + e.getMessage(), e);
        }
    }

    private String resolveApiKey() {
        String k = embeddingProperties.getApiKey();
        if (k != null && !k.isBlank()) {
            return k;
        }
        return llmProperties.getApiKey();
    }

    private static String trimTrailingSlash(String u) {
        if (u == null || u.isEmpty()) {
            return "";
        }
        return u.endsWith("/") ? u.substring(0, u.length() - 1) : u;
    }
}
