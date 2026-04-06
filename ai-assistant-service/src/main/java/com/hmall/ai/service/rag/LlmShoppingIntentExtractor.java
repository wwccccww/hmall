package com.hmall.ai.service.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmall.ai.llm.LlmClient;
import com.hmall.ai.llm.LlmProperties;
import com.hmall.common.catalog.ItemCategoryCatalog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 使用 LLM 从自然语言抽取结构化检索意图；失败或未启用时返回 empty。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmShoppingIntentExtractor {

    private static final String SYSTEM_PROMPT_HEAD = ""
            + "你是电商搜索意图解析器，只根据用户原话抽取字段，不得编造。"
            + "minPrice、maxPrice 必须为整数「分」，成对出现或均为 null（例如 2000 元≈min=max=200000，区间需换算为分）。"
            + "未提及的字段必须为 null 或省略；字符串禁止输出空串。"
            + "brand：常见中文品牌名（如 华为）。category：只能是下列之一，或与下列之一明显同义时填对应项，否则必须 null：";

    private static final String SYSTEM_PROMPT_TAIL = ""
            + "。例如用户说运动鞋/跑鞋/凉鞋/鞋子 → 休闲鞋；说电视/彩电 → 曲面电视；说行李箱 → 拉杆箱；说固态硬盘/SSD → 硬盘。"
            + "color、size：用户原词节选（如 蓝、42）；searchKey：去掉寒暄后的核心检索语，可 null。"
            + "只输出 JSON，键名严格：searchKey,minPrice,maxPrice,category,brand,color,size。无 Markdown、无代码围栏。";

    private final LlmClient llmClient;
    private final LlmProperties llmProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${hm.ai.rag.llm-intent-enabled:false}")
    private boolean intentEnabled;

    public Optional<ShoppingIntentLlmFields> extract(String userMessage) {
        if (!intentEnabled) {
            return Optional.empty();
        }
        if (userMessage == null || userMessage.isBlank()) {
            return Optional.empty();
        }
        String apiKey = llmProperties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }
        try {
            String raw = llmClient.chatWithSystem(systemPrompt(), "用户原话：\n" + userMessage.trim(), 0.1);
            String json = stripJsonFence(raw);
            log.info("LLM shopping intent JSON: {}", json);
            ShoppingIntentLlmFields fields = parseFields(json);
            sanitize(fields);
            return Optional.of(fields);
        } catch (Exception e) {
            log.warn("LLM shopping intent extract failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static String systemPrompt() {
        return SYSTEM_PROMPT_HEAD + ItemCategoryCatalog.canonicalListForPrompt() + SYSTEM_PROMPT_TAIL;
    }

    private static String stripJsonFence(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (s.startsWith("```")) {
            int nl = s.indexOf('\n');
            if (nl > 0) {
                s = s.substring(nl + 1);
            }
            int fence = s.lastIndexOf("```");
            if (fence >= 0) {
                s = s.substring(0, fence);
            }
        }
        return s.trim();
    }

    private ShoppingIntentLlmFields parseFields(String json) throws Exception {
        JsonNode n = objectMapper.readTree(json);
        ShoppingIntentLlmFields f = new ShoppingIntentLlmFields();
        if (n.hasNonNull("searchKey")) {
            f.setSearchKey(n.get("searchKey").asText().trim());
        }
        if (n.hasNonNull("category")) {
            f.setCategory(n.get("category").asText().trim());
        }
        if (n.hasNonNull("brand")) {
            f.setBrand(n.get("brand").asText().trim());
        }
        if (n.hasNonNull("color")) {
            f.setColor(n.get("color").asText().trim());
        }
        if (n.hasNonNull("size")) {
            f.setSize(n.get("size").asText().trim());
        }
        if (n.has("minPrice") && !n.get("minPrice").isNull()) {
            f.setMinPrice(n.get("minPrice").asInt());
        }
        if (n.has("maxPrice") && !n.get("maxPrice").isNull()) {
            f.setMaxPrice(n.get("maxPrice").asInt());
        }
        return f;
    }

    private static void sanitize(ShoppingIntentLlmFields f) {
        if (f.getSearchKey() != null && f.getSearchKey().isBlank()) {
            f.setSearchKey(null);
        }
        if (f.getCategory() != null && f.getCategory().isBlank()) {
            f.setCategory(null);
        }
        if (f.getBrand() != null && f.getBrand().isBlank()) {
            f.setBrand(null);
        }
        if (f.getColor() != null && f.getColor().isBlank()) {
            f.setColor(null);
        }
        if (f.getSize() != null && f.getSize().isBlank()) {
            f.setSize(null);
        }
        Integer min = f.getMinPrice();
        Integer max = f.getMaxPrice();
        if (min != null && max != null) {
            if (min < 0) {
                min = 0;
                f.setMinPrice(0);
            }
            if (max < 0) {
                f.setMaxPrice(null);
                f.setMinPrice(null);
            } else if (min > max) {
                f.setMinPrice(max);
                f.setMaxPrice(min);
            }
            if (f.getMaxPrice() != null && f.getMaxPrice() > 100_000_000) {
                f.setMaxPrice(100_000_000);
            }
        } else {
            f.setMinPrice(null);
            f.setMaxPrice(null);
        }
    }
}
