package com.hmall.ai.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiCompatibleLlmClient implements LlmClient {

    private final LlmProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private WebClient client() {
        return WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + (props.getApiKey() == null ? "" : props.getApiKey()))
                .build();
    }

    @Override
    public String chat(String userMessage) {
        Map<String, Object> body = baseBody(userMessage);
        body.put("stream", false);
        return client().post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(props.getTimeoutMs()))
                .flatMap(this::extractContent)
                .blockOptional()
                .orElse("");
    }

    @Override
    public Flux<String> streamChat(String userMessage) {
        Map<String, Object> body = baseBody(userMessage);
        body.put("stream", true);

        return client().post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofMillis(props.getTimeoutMs()))
                .flatMap(this::extractDeltaFromSseLine)
                .onErrorResume(e -> {
                    log.warn("LLM stream failed, fallback to sync: {}", e.getMessage());
                    return Flux.fromIterable(splitToChunks(chat(userMessage), 30));
                });
    }

    private Map<String, Object> baseBody(String userMessage) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", props.getModel());
        body.put("temperature", 0.2);
        body.put("messages", List.of(
                Map.of("role", "system", "content", "你是电商导购助手。回答要简洁、可执行，避免编造；如缺少信息请明确说明。"
                        + "勿在回答中输出 JSON 或 Markdown 代码块；结构化数据由前端卡片展示时用自然语言概括即可。"),
                Map.of("role", "user", "content", userMessage)
        ));
        return body;
    }

    private Mono<String> extractContent(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            return Mono.just(content.isMissingNode() ? "" : content.asText(""));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    /**
     * OpenAI stream 每行类似：data: {...} 或 data: [DONE]
     */
    private Mono<String> extractDeltaFromSseLine(String line) {
        if (line == null) {
            return Mono.empty();
        }
        String trimmed = line.trim();
        if (!trimmed.startsWith("data:")) {
            return Mono.empty();
        }
        String data = trimmed.substring(5).trim();
        if (data.isEmpty() || "[DONE]".equals(data)) {
            return Mono.empty();
        }
        try {
            JsonNode root = objectMapper.readTree(data);
            JsonNode delta = root.path("choices").path(0).path("delta").path("content");
            String text = delta.isMissingNode() ? "" : delta.asText("");
            return text.isEmpty() ? Mono.empty() : Mono.just(text);
        } catch (Exception e) {
            return Mono.empty();
        }
    }

    private List<String> splitToChunks(String text, int chunkSize) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        int n = Math.max(1, chunkSize);
        int len = text.length();
        java.util.ArrayList<String> out = new java.util.ArrayList<>((len + n - 1) / n);
        for (int i = 0; i < len; i += n) {
            out.add(text.substring(i, Math.min(len, i + n)));
        }
        return out;
    }
}

