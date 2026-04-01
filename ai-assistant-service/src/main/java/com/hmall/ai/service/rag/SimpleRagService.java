package com.hmall.ai.service.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimpleRagService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 第一版：从 item-service 拉一页商品，在本地做简单命中（关键词包含），作为“检索结果上下文”。\n
     * 后续可替换为 ES/向量检索。
     */
    public RagResult buildRagPrompt(String userMessage) {
        List<Map<String, Object>> sources = new ArrayList<>();
        String context = "";
        try {
            List<Map<String, Object>> items = fetchCandidatesFromItemService(userMessage);
            if (!items.isEmpty()) {
                sources.addAll(items);
                context = buildContextText(items);
            }
        } catch (Exception e) {
            log.warn("RAG candidates fetch failed: {}", e.getMessage());
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("你是电商导购助手，请用中文回答。\n");
        prompt.append("规则：\n");
        prompt.append("- 只能基于【候选商品信息】做推荐与解释；如果信息不足，请说明“未找到依据”，并询问用户补充预算/用途/偏好。\n");
        prompt.append("- 输出结构：\n");
        prompt.append("  1) 结论（1-2 句话）\n");
        prompt.append("  2) 推荐清单（3-5 个，含：商品ID、名称、推荐理由、适合人群）\n");
        prompt.append("  3) 购买建议（对比维度/避坑）\n\n");
        prompt.append("【用户问题】\n").append(userMessage).append("\n\n");
        prompt.append("【候选商品信息】\n").append(context).append("\n");

        return new RagResult(prompt.toString(), sources);
    }

    private List<Map<String, Object>> fetchCandidatesFromItemService(String userMessage) throws Exception {
        // 使用服务名走网关注册发现：需要 nacos discovery + Spring Cloud LoadBalancer 额外配置时再升级
        // 这里先用 http://item-service:8081 以容器网络为默认（本地也可通过 hosts/compose 访问）
        WebClient webClient = WebClient.builder().baseUrl("http://item-service:8081").build();
        String json = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/items/page")
                        .queryParam("pageNo", 1)
                        .queryParam("pageSize", 50)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        if (json == null || json.isBlank()) {
            return List.of();
        }
        JsonNode root = objectMapper.readTree(json);
        JsonNode list = root.path("list");
        if (!list.isArray()) {
            return List.of();
        }

        String keyword = normalizeKeyword(userMessage);
        List<Map<String, Object>> out = new ArrayList<>();
        for (JsonNode node : list) {
            String name = node.path("name").asText("");
            String desc = node.path("desc").asText("");
            if (keyword.isBlank() || (name + " " + desc).toLowerCase().contains(keyword)) {
                Map<String, Object> m = new HashMap<>();
                m.put("type", "item");
                m.put("id", node.path("id").asLong());
                m.put("name", name);
                m.put("price", node.path("price").asLong());
                m.put("category", node.path("category").asText(""));
                out.add(m);
            }
            if (out.size() >= 8) break;
        }
        return out;
    }

    private String buildContextText(List<Map<String, Object>> items) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> it : items) {
            sb.append("- 商品ID=").append(it.get("id"))
              .append("；名称=").append(it.get("name"))
              .append("；价格=").append(it.get("price"))
              .append("；类目=").append(it.getOrDefault("category", ""))
              .append("\n");
        }
        return sb.toString();
    }

    private String normalizeKeyword(String userMessage) {
        if (userMessage == null) return "";
        String s = userMessage.trim().toLowerCase();
        // 简单截取：避免把整段问题当关键词
        if (s.length() > 12) {
            s = s.substring(0, 12);
        }
        return s;
    }

    public static class RagResult {
        private final String prompt;
        private final List<Map<String, Object>> sources;

        public RagResult(String prompt, List<Map<String, Object>> sources) {
            this.prompt = prompt;
            this.sources = sources;
        }

        public String prompt() {
            return prompt;
        }

        public List<Map<String, Object>> sources() {
            return sources;
        }
    }
}

