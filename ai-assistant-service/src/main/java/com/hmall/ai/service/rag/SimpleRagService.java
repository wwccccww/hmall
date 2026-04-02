package com.hmall.ai.service.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
     * item-service 访问地址：\n
     * - Docker Compose：默认 http://item-service:8081\n
     * - 本地联调：可覆盖为 http://localhost:8081 或走网关
     */
    @Value("${hm.ai.rag.item-base-url:${HM_AI_ITEM_BASE_URL:http://item-service:8081}}")
    private String itemBaseUrl;

    /**
     * promotion-service，用于 GET /coupons（公开进行中券列表）
     */
    @Value("${hm.ai.rag.promotion-base-url:${HM_AI_PROMOTION_BASE_URL:http://promotion-service:8087}}")
    private String promotionBaseUrl;

    @Value("${hm.ai.rag.item-max-pages:2}")
    private int itemMaxPages;

    @Value("${hm.ai.rag.public-coupon-max:15}")
    private int publicCouponMax;

    private static final int ITEM_PAGE_SIZE = 50;

    /**
     * 从 item-service 拉多页商品 + promotion-service 公开券摘要，本地关键词过滤后组装 Prompt。
     */
    public RagResult buildRagPrompt(String userMessage) {
        List<Map<String, Object>> sources = new ArrayList<>();
        String itemContext = "";
        String couponContext = "";

        try {
            List<Map<String, Object>> items = fetchCandidatesFromItemService(userMessage);
            if (!items.isEmpty()) {
                sources.addAll(items);
                itemContext = buildItemContextText(items);
            }
        } catch (Exception e) {
            log.warn("RAG item candidates fetch failed: {}", e.getMessage());
        }

        try {
            List<Map<String, Object>> coupons = fetchPublicCoupons();
            if (!coupons.isEmpty()) {
                sources.addAll(coupons);
                couponContext = buildCouponContextText(coupons);
            }
        } catch (Exception e) {
            log.warn("RAG public coupons fetch failed: {}", e.getMessage());
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("你是电商导购助手，请用中文回答。\n");
        prompt.append("规则：\n");
        prompt.append("- 只能基于【候选商品信息】与【公开进行中优惠券摘要】做推荐与解释；如果信息不足，请说明“未找到依据”，并询问用户补充预算/用途/偏好。\n");
        prompt.append("- 不要编造未在上下文中出现的商品 ID、价格或券规则。\n");
        prompt.append("- 输出结构：\n");
        prompt.append("  1) 结论（1-2 句话）\n");
        prompt.append("  2) 推荐清单（3-5 个，含：商品ID、名称、推荐理由、适合人群）\n");
        prompt.append("  3) 购买建议（对比维度/避坑）；如上下文含可用券，可简要说明门槛与类型。\n\n");
        prompt.append("【用户问题】\n").append(userMessage).append("\n\n");
        prompt.append("【候选商品信息】\n").append(itemContext.isEmpty() ? "（无）\n" : itemContext).append("\n");
        prompt.append("【公开进行中优惠券摘要】\n").append(couponContext.isEmpty() ? "（无）\n" : couponContext).append("\n");

        return new RagResult(prompt.toString(), sources);
    }

    private List<Map<String, Object>> fetchCandidatesFromItemService(String userMessage) throws Exception {
        String baseUrl = itemBaseUrl == null ? "http://item-service:8081" : itemBaseUrl;
        WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();

        List<JsonNode> allNodes = new ArrayList<>();
        int pages = Math.max(1, Math.min(itemMaxPages, 10));
        for (int pageNo = 1; pageNo <= pages; pageNo++) {
            final int page = pageNo;
            String json = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/items/page")
                            .queryParam("pageNo", page)
                            .queryParam("pageSize", ITEM_PAGE_SIZE)
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            if (json == null || json.isBlank()) {
                break;
            }
            JsonNode root = objectMapper.readTree(json);
            JsonNode list = root.path("list");
            if (!list.isArray() || list.isEmpty()) {
                break;
            }
            for (JsonNode node : list) {
                allNodes.add(node);
            }
        }

        String keyword = normalizeKeyword(userMessage);
        List<Map<String, Object>> out = new ArrayList<>();
        List<Map<String, Object>> firstN = new ArrayList<>();
        for (JsonNode node : allNodes) {
            String name = node.path("name").asText("");
            String desc = node.path("desc").asText("");
            if (firstN.size() < 8) {
                firstN.add(toItemSource(node, name));
            }
            if (keyword.isBlank() || (name + " " + desc).toLowerCase().contains(keyword)) {
                out.add(toItemSource(node, name));
            }
            if (out.size() >= 8) {
                break;
            }
        }
        return out.isEmpty() ? firstN : out;
    }

    private List<Map<String, Object>> fetchPublicCoupons() throws Exception {
        String base = promotionBaseUrl == null ? "http://promotion-service:8087" : promotionBaseUrl;
        WebClient webClient = WebClient.builder().baseUrl(base).build();
        String json = webClient.get()
                .uri("/coupons")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        if (json == null || json.isBlank()) {
            return List.of();
        }
        JsonNode root = objectMapper.readTree(json);
        if (!root.isArray()) {
            return List.of();
        }
        int max = Math.max(1, Math.min(publicCouponMax, 50));
        List<Map<String, Object>> list = new ArrayList<>();
        int i = 0;
        for (JsonNode node : root) {
            if (i++ >= max) {
                break;
            }
            list.add(toPublicCouponSource(node));
        }
        return list;
    }

    private Map<String, Object> toPublicCouponSource(JsonNode node) {
        Map<String, Object> m = new HashMap<>();
        m.put("type", "public_coupon");
        m.put("id", node.path("id").asLong());
        m.put("name", node.path("name").asText(""));
        m.put("discountType", node.path("type").asInt(0));
        m.put("discountValue", node.path("discountValue").asInt(0));
        m.put("threshold", node.path("threshold").asInt(0));
        m.put("stock", node.path("stock").asInt(0));
        if (node.hasNonNull("beginTime")) {
            m.put("beginTime", node.path("beginTime").asText(""));
        }
        if (node.hasNonNull("endTime")) {
            m.put("endTime", node.path("endTime").asText(""));
        }
        return m;
    }

    private Map<String, Object> toItemSource(JsonNode node, String name) {
        Map<String, Object> m = new HashMap<>();
        m.put("type", "item");
        m.put("id", node.path("id").asLong());
        m.put("name", name);
        m.put("price", node.path("price").asLong());
        m.put("category", node.path("category").asText(""));
        return m;
    }

    private String buildItemContextText(List<Map<String, Object>> items) {
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

    private String buildCouponContextText(List<Map<String, Object>> coupons) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> c : coupons) {
            Object dt = c.get("discountType");
            boolean fullReduce = dt instanceof Number && ((Number) dt).intValue() == 1;
            String typeLabel = fullReduce ? "满减" : "折扣";
            sb.append("- 券ID=").append(c.get("id"))
                    .append("；名称=").append(c.get("name"))
                    .append("；类型=").append(typeLabel)
                    .append("；优惠值=").append(c.get("discountValue"))
                    .append("；门槛(分)=").append(c.get("threshold"))
                    .append("；剩余库存=").append(c.get("stock"))
                    .append("\n");
        }
        return sb.toString();
    }

    private String normalizeKeyword(String userMessage) {
        if (userMessage == null) {
            return "";
        }
        String s = userMessage.trim().toLowerCase();
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
