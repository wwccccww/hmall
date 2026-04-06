package com.hmall.ai.service.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmall.ai.knowledge.KnowledgeEsIndexService;
import com.hmall.ai.knowledge.KnowledgeEsIndexService.KnowledgeHit;
import com.hmall.ai.knowledge.KnowledgeVectorSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimpleRagService {

    private final ShoppingIntentParseService shoppingIntentParseService;
    private final ObjectProvider<KnowledgeVectorSearchService> knowledgeVectorSearch;

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

    @Value("${hm.ai.rag.use-elasticsearch:true}")
    private boolean useElasticsearch;

    @Value("${hm.ai.rag.search-top-k:20}")
    private int searchTopK;

    @Value("${hm.ai.rag.prompt-max-items:12}")
    private int promptMaxItems;

    /** 前端商城根 URL，用于生成可点击的绝对商品链接；留空则只下发相对路径 productPath */
    @Value("${hm.ai.mall.front-base-url:${HM_AI_MALL_FRONT_BASE_URL:}}")
    private String mallFrontBaseUrl;

    /** 写入 prompt 的向量知识库摘录总长上限 */
    @Value("${hm.ai.knowledge.max-prompt-chars:3600}")
    private int knowledgeMaxPromptChars;

    /** 商品分页大小 */
    private static final int ITEM_PAGE_SIZE = 50;

    /**
     * 从 ES（/search/list）优先召回商品，失败或无结果时回退 /items/page；附带公开券摘要。
     * @param userMessage 用户问题
     * @return RAG 结果
     */
    public RagResult buildRagPrompt(String userMessage) {
        List<Map<String, Object>> sources = new ArrayList<>();
        String itemContext = "";
        String couponContext = "";
        String kbContext = "";

        try {
            KnowledgeVectorSearchService kb = knowledgeVectorSearch.getIfAvailable();
            if (kb != null) {
                List<KnowledgeHit> hits = kb.search(userMessage);
                kbContext = appendKnowledgeHits(sources, hits);
            }
        } catch (Exception e) {
            log.warn("RAG knowledge vector search failed: {}", e.getMessage());
        }

        try {
            List<Map<String, Object>> items = fetchItemCandidates(userMessage);
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
        prompt.append("- 只能基于【候选商品信息】、【公开进行中优惠券摘要】与【知识库向量检索】中的内容做推荐与解释；若用户问价格/库存/能否购买，以候选商品为准；知识库多为规则与常见问题，与实时促销冲突时以商品与券摘要为准。\n");
        prompt.append("- 不要编造未在上下文中出现的商品 ID、价格或券规则。\n");
        prompt.append("- 若用户已在问题里写明硬性偏好（如颜色「金色」、预算区间、品牌），但候选商品均不满足：须在结论中明确「当前候选中没有符合该条件的商品」，可把清单作为外观/定位相近的替代选项，并说明它们不满足哪一条。\n");
        prompt.append("- 切勿让用户重复他已经说过的条件（例如已问金色却仍写「请补充金色偏好」）；应改为：是否接受其它颜色/提高预算/换品牌，或说明可到卖场再看新SKU。\n");
        prompt.append("- 勿在回答中输出 JSON、Markdown 代码块或整段复制【候选商品信息】原文；商品详情由对话下方的卡片展示，正文只做推荐与说明。\n");
        prompt.append("- 输出结构：\n");
        prompt.append("  1) 结论（1-2 句话）\n");
        prompt.append("  2) 推荐清单（3-5 个，含：商品ID、名称、推荐理由、适合人群）；若无完全匹配，标题或首句注明「以下为替代选项/近似款」。\n");
        prompt.append("  3) 购买建议（对比维度/避坑）；如上下文含可用券，可简要说明门槛与类型。\n");
        prompt.append("  4) 结尾用一句话提醒：用户可在对话下方的商品卡片中「查看商品」或「加入购物车」。\n\n");
        prompt.append("【用户问题】\n").append(userMessage).append("\n\n");
        prompt.append("【候选商品信息】\n").append(itemContext.isEmpty() ? "（无）\n" : itemContext).append("\n");
        prompt.append("【公开进行中优惠券摘要】\n").append(couponContext.isEmpty() ? "（无）\n" : couponContext).append("\n");
        prompt.append("【知识库向量检索（平台说明/FAQ，非实时价）】\n").append(kbContext.isEmpty() ? "（无）\n" : kbContext).append("\n");

        return new RagResult(prompt.toString(), sources);
    }

    /**
     * 将知识库命中写入 sources，并返回拼入 prompt 的纯文本（受字数上限截断）。
     */
    private String appendKnowledgeHits(
            List<Map<String, Object>> sources,
            List<KnowledgeHit> hits) {
        if (hits == null || hits.isEmpty()) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        int remaining = Math.max(500, knowledgeMaxPromptChars);
        int idx = 1;
        for (KnowledgeHit h : hits) {
            Map<String, Object> src = new LinkedHashMap<>();
            src.put("type", "kb_chunk");
            src.put("chunkId", h.chunkId());
            src.put("sourceFile", h.sourceFile());
            src.put("score", h.score());
            String excerpt = h.text();
            if (excerpt != null && excerpt.length() > 1200) {
                excerpt = excerpt.substring(0, 1200) + "…";
            }
            src.put("text", excerpt);
            sources.add(src);
            String block = "(" + idx + ") [score=" + String.format("%.3f", h.score()) + "] " + excerpt + "\n\n";
            idx++;
            if (block.length() >= remaining) {
                text.append(block, 0, Math.min(block.length(), remaining));
                break;
            }
            text.append(block);
            remaining -= block.length();
            if (remaining <= 0) {
                break;
            }
        }
        return text.toString().trim();
    }

    /**
     * 从 ES（/search/list）优先召回商品，失败或无结果时回退 /items/page。
     * @param userMessage 用户关键词
     * @return 商品卡片列表
     * @throws Exception 调用 ES 或 item-service 失败
     */
    private List<Map<String, Object>> fetchItemCandidates(String userMessage) throws Exception {
        if (useElasticsearch) {
            try {
                List<Map<String, Object>> fromEs = fetchFromElasticsearch(userMessage);
                if (!fromEs.isEmpty()) {
                    return trimForPrompt(fromEs);
                }
                log.info("ES /search/list 无结果，回退 /items/page");
            } catch (Exception e) {
                log.warn("ES /search/list 调用失败，回退 /items/page: {}", e.getMessage());
            }
        }
        return trimForPrompt(fetchCandidatesFromItemService(userMessage));
    }

    private List<Map<String, Object>> trimForPrompt(List<Map<String, Object>> items) {
        int max = Math.max(4, Math.min(promptMaxItems, 50));
        if (items.size() <= max) {
            return items;
        }
        return new ArrayList<>(items.subList(0, max));
    }

    /**
     * 调用 item-service {@code GET /search/list}，与用户意图轻量解析参数对齐。
     */
    private List<Map<String, Object>> fetchFromElasticsearch(String userMessage) throws Exception {
        String baseUrl = itemBaseUrl == null ? "http://item-service:8081" : itemBaseUrl;
        WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();
        ShoppingIntentParser.Parsed parsed = shoppingIntentParseService.parse(userMessage);

        int size = Math.max(5, Math.min(searchTopK, 50));
        String json = webClient.get()
                .uri((UriBuilder uriBuilder) -> {
                    UriBuilder b = uriBuilder
                            .path("/search/list")
                            .queryParam("pageNo", 1)
                            .queryParam("pageSize", size)
                            .queryParam("status", 1);
                    if (parsed.getSearchKey() != null && !parsed.getSearchKey().isBlank()) {
                        b.queryParam("key", parsed.getSearchKey());
                    }
                    if (parsed.getBrand() != null && !parsed.getBrand().isBlank()) {
                        b.queryParam("brand", parsed.getBrand());
                    }
                    if (parsed.getCategory() != null && !parsed.getCategory().isBlank()) {
                        b.queryParam("category", parsed.getCategory());
                    }
                    //TODO 如果只有最大值或者最小值，也加入条件筛选
                    if (parsed.hasPriceBand()) {
                        b.queryParam("minPrice", parsed.getMinPrice());
                        b.queryParam("maxPrice", parsed.getMaxPrice());
                    }
                    return b.build();
                })
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (json == null || json.isBlank()) {
            return List.of();
        }
        JsonNode root = objectMapper.readTree(json);
        JsonNode list = root.path("list");
        if (!list.isArray() || list.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (JsonNode node : list) {
            out.add(fromSearchHitToItemSource(node));
        }
        return out;
    }

    /**
     * 将 ES 搜索结果转换为商品卡片格式
     * @param node ES 搜索结果节点
     * @return 商品卡片
     */
    private Map<String, Object> fromSearchHitToItemSource(JsonNode node) {
        Map<String, Object> m = new HashMap<>();
        m.put("type", "item");
        m.put("id", node.path("id").asLong());
        m.put("name", node.path("name").asText(""));
        m.put("price", node.path("price").asLong());
        m.put("category", node.path("category").asText(""));
        m.put("brand", node.path("brand").asText(""));
        m.put("spec", node.path("spec").asText(""));
        if (node.hasNonNull("image")) {
            m.put("image", node.path("image").asText(""));
        }
        if (node.hasNonNull("specColor")) {
            m.put("specColor", node.path("specColor").asText(""));
        }
        if (node.hasNonNull("specSize")) {
            m.put("specSize", node.path("specSize").asText(""));
        }
        // 填充 spec 字段的 JSON 字段
        enrichSpecJsonFields(m, node.path("spec").asText(""));
        m.put("source", "elasticsearch");
        // 填充购买字段
        enrichPurchaseFields(m);
        return m;
    }

    /**
     * 从 item-service 获取商品卡片
     * @param userMessage 用户关键词
     * @return 商品卡片列表
     */
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
                firstN.add(toItemSourceFromPage(node, name));
            }
            if (keyword.isBlank() || (name + " " + desc).toLowerCase().contains(keyword)) {
                out.add(toItemSourceFromPage(node, name));
            }
            if (out.size() >= 8) {
                break;
            }
        }
        return out.isEmpty() ? firstN : out;
    }

    /**
     * 从 promotion-service 获取公开优惠券
     * @return 公开优惠券列表
     * @throws Exception 从 promotion-service 获取公开优惠券失败
     */
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

    /**
     * 将 promotion-service 优惠券节点转换为 RAG 商品卡片
     * @param node 优惠券节点
     * @return 优惠券卡片
     */
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

    /**
     * 将 item-service 商品节点转换为 RAG 商品卡片
     * @param node 商品节点
     * @param name 商品名称
     * @return 商品卡片
     */
    private Map<String, Object> toItemSourceFromPage(JsonNode node, String name) {
        Map<String, Object> m = new HashMap<>();
        m.put("type", "item");
        m.put("id", node.path("id").asLong());
        m.put("name", name);
        m.put("price", node.path("price").asLong());
        m.put("category", node.path("category").asText(""));
        m.put("brand", node.path("brand").asText(""));
        m.put("spec", node.path("spec").asText(""));
        if (node.hasNonNull("image")) {
            m.put("image", node.path("image").asText(""));
        }
        enrichSpecJsonFields(m, node.path("spec").asText(""));
        m.put("source", "mysql_page");
        enrichPurchaseFields(m);
        return m;
    }

    /**
     * 填充购买字段
     * @param m 商品卡片
     */
    private void enrichPurchaseFields(Map<String, Object> m) {
        Object idObj = m.get("id");
        if (idObj == null) {
            return;
        }
        long id = idObj instanceof Number ? ((Number) idObj).longValue() : Long.parseLong(String.valueOf(idObj));
        m.put("productPath", "/product/" + id);
        String base = normalizeMallFrontBase();
        if (base != null && !base.isBlank()) {
            m.put("productUrl", base + "/product/" + id);
        }
        int priceCents = 0;
        Object p = m.get("price");
        if (p instanceof Number) {
            priceCents = ((Number) p).intValue();
        }
        String specStr = m.get("spec") == null ? "{}" : String.valueOf(m.get("spec"));
        if (specStr.isBlank()) {
            specStr = "{}";
        }
        Map<String, Object> cart = new LinkedHashMap<>();
        cart.put("itemId", id);
        cart.put("num", 1);
        cart.put("name", String.valueOf(m.getOrDefault("name", "")));
        cart.put("price", priceCents);
        cart.put("image", String.valueOf(m.getOrDefault("image", "")));
        cart.put("category", String.valueOf(m.getOrDefault("category", "")));
        cart.put("spec", specStr);
        m.put("addToCart", cart);
    }

    /**
     * 规范商城前端基础 URL
     * @return 规范后的商城前端基础 URL
     */
    private String normalizeMallFrontBase() {
        if (mallFrontBaseUrl == null) {
            return null;
        }
        String s = mallFrontBaseUrl.trim();
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s.isEmpty() ? null : s;
    }

    /** 与 ItemDoc 一致：把 {"颜色":...,"尺寸":...} 解析进 map，便于 prompt 展示 */
    private void enrichSpecJsonFields(Map<String, Object> m, String specStr) {
        if (specStr == null || specStr.isBlank() || !specStr.trim().startsWith("{")) {
            return;
        }
        try {
            JsonNode o = objectMapper.readTree(specStr);
            if (o.hasNonNull("颜色")) {
                m.put("specColor", o.get("颜色").asText(""));
            }
            if (o.hasNonNull("尺寸")) {
                m.put("specSize", o.get("尺寸").asText(""));
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 构建商品卡片上下文文本
     * @param items 商品卡片列表
     * @return 上下文文本
     */
    private String buildItemContextText(List<Map<String, Object>> items) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> it : items) {
            sb.append("- 商品ID=").append(it.get("id"))
                    .append("；名称=").append(it.get("name"))
                    .append("；价格(分)=").append(it.get("price"))
                    .append("；类目=").append(it.getOrDefault("category", ""))
                    .append("；品牌=").append(it.getOrDefault("brand", ""))
                    .append("；规格=").append(it.getOrDefault("spec", ""))
                    .append("；颜色=").append(it.getOrDefault("specColor", ""))
                    .append("；尺寸=").append(it.getOrDefault("specSize", ""))
                    .append("\n");
        }
        return sb.toString();
    }

    /**
     * 构建优惠券卡片上下文文本
     * @param coupons 优惠券卡片列表
     * @return 上下文文本
     */
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

    /**
     * 规范用户关键词
     * @param userMessage 用户关键词原始文本
     * @return 规范后的关键词文本
     */
    //TODO 规范关键词，例如去停用词、去标点符号等
    // 这里简单地去停用词、去标点符号、取前12个字符
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

    /** RAG 结果 */
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
