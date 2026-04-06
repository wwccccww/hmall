package com.hmall.ai.service.rag;

import com.hmall.common.catalog.ItemCategoryCatalog;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将用户自然语言轻量解析为 item-service「/search/list」可用的查询参数（宽召回为主）。
 */
public final class ShoppingIntentParser {

    private static final Pattern RANGE_YUAN = Pattern.compile(
            "(\\d+(?:\\.\\d+)?)\\s*[-到至~～]\\s*(\\d+(?:\\.\\d+)?)\\s*(?:元|块|块钱)?");
    private static final Pattern SINGLE_YUAN = Pattern.compile(
            "(\\d+(?:\\.\\d+)?)\\s*(?:元|块|块钱)");
    private static final Pattern WAN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*万\\s*(?:元|块|块钱)?");
    private static final Pattern QIAN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*千\\s*(?:元|块|块钱)?");

    private static final Map<String, String> BRAND_HINTS = new LinkedHashMap<>();

    static {
        BRAND_HINTS.put("华为", "华为");
        BRAND_HINTS.put("小米", "小米");
        BRAND_HINTS.put("苹果", "苹果");
        BRAND_HINTS.put("iphone", "苹果");
        BRAND_HINTS.put("apple", "苹果");
        BRAND_HINTS.put("oppo", "OPPO");
        BRAND_HINTS.put("vivo", "vivo");
        BRAND_HINTS.put("三星", "三星");
        BRAND_HINTS.put("荣耀", "荣耀");
    }

    private ShoppingIntentParser() {
    }

    public static Parsed parse(String userMessage) {
        Parsed p = new Parsed();
        if (userMessage == null || userMessage.isBlank()) {
            p.searchKey = "";
            return p;
        }
        String text = userMessage.trim();
        p.searchKey = text;

        Matcher range = RANGE_YUAN.matcher(text);
        if (range.find()) {
            double lo = Double.parseDouble(range.group(1));
            double hi = Double.parseDouble(range.group(2));
            p.minPrice = yuanToFen(lo);
            p.maxPrice = yuanToFen(hi);
        } else {
            Matcher wan = WAN.matcher(text);
            if (wan.find()) {
                double w = Double.parseDouble(wan.group(1));
                double center = w * 10000;
                bandAroundYuan(p, center, 0.2);
            } else {
                Matcher qian = QIAN.matcher(text);
                if (qian.find()) {
                    double k = Double.parseDouble(qian.group(1));
                    bandAroundYuan(p, k * 1000, 0.15);
                } else {
                    Matcher single = SINGLE_YUAN.matcher(text);
                    if (single.find()) {
                        double y = Double.parseDouble(single.group(1));
                        bandAroundYuan(p, y, 0.15);
                    }
                }
            }
        }

        // 品牌提示
        String lower = text.toLowerCase();
        for (Map.Entry<String, String> e : BRAND_HINTS.entrySet()) {
            if (text.contains(e.getKey()) || lower.contains(e.getKey().toLowerCase())) {
                p.brand = e.getValue();
                break;
            }
        }

        p.specColor = ItemCategoryCatalog.inferColorKeyword(text);
        p.category = ItemCategoryCatalog.inferCategoryFromUserText(text);

        // 去掉套话，用核心词检索（整句「请给我推荐鞋子」容易宽召回到无关商品或 0 命中后落库分页）
        String core = simplifyKeyForEs(text);
        if (core != null && !core.isBlank() && !core.equals(text)) {
            p.searchKey = core;
        }

        return p;
    }

    /**
     * 压缩自然语言检索词：去掉礼貌/导购套话，保留品类与预算相关表述。
     */
    private static String simplifyKeyForEs(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        String k = raw.trim();
        // 按长度降序优先替换长短语，避免残留碎片
        String[] noise = {
                "请给我推荐", "麻烦给我推荐", "帮我推荐一下", "帮我推荐", "给我推荐一下", "给我推荐",
                "请推荐", "推荐一下", "推荐点", "推荐些", "推荐",
                "我想买", "我要买", "想买", "要买", "有没有卖", "有没有", "能不能推荐",
                "帮我搜", "帮我找", "搜一下", "查一下", "查询",
                "请介绍", "介绍一下", "介绍"
        };
        for (String n : noise) {
            k = k.replace(n, " ");
        }
        k = k.replaceAll("[\\p{Punct}，。！？、；：\\s]+", " ").trim();
        if (k.length() < 2) {
            return raw.trim();
        }
        return k;
    }

    /**
     * 是否应优先走本地 {@code GET /search/list} 商品检索（与 {@link com.hmall.item.controller.SearchController} 类目/color 过滤对齐），
     * 而不要仅用百炼智能体回答：百炼侧命中片段可能只强调「蓝色」，忽略「鞋子」等品类约束。
     */
    public static boolean prefersElasticsearchItemSearch(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return false;
        }
        String t = userMessage.trim();
        if (ItemCategoryCatalog.inferColorKeyword(t) != null) {
            return true;
        }
        if (RANGE_YUAN.matcher(t).find()
                || SINGLE_YUAN.matcher(t).find()
                || WAN.matcher(t).find()
                || QIAN.matcher(t).find()) {
            return true;
        }
        String lower = t.toLowerCase();
        for (String k : BRAND_HINTS.keySet()) {
            if (t.contains(k) || lower.contains(k.toLowerCase())) {
                return true;
            }
        }
        if (t.contains("靴")) {
            return true;
        }
        if (hasShoeProductKeyword(t)) {
            return true;
        }
        return hasPhoneProductKeyword(t);
    }

    private static boolean hasShoeProductKeyword(String key) {
        if (!key.contains("鞋")) {
            return false;
        }
        String[] exclude = {"鞋柜", "鞋架", "鞋垫", "鞋带", "鞋油", "鞋盒", "鞋刷", "鞋袜", "鞋拔"};
        for (String ex : exclude) {
            if (key.contains(ex)) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasPhoneProductKeyword(String key) {
        if (!key.contains("手机")) {
            return false;
        }
        String[] exclude =
                {"手机壳", "手机套", "手机膜", "手机支架", "手机链", "手机包", "手机挂绳", "手机座", "手机袋"};
        for (String ex : exclude) {
            if (key.contains(ex)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 生成价格范围
     * @param p 解析结果
     * @param centerYuan 中心价格（元）
     * @param ratio 价格范围比例
     */
    private static void bandAroundYuan(Parsed p, double centerYuan, double ratio) {
        double lo = Math.max(0, centerYuan * (1 - ratio));
        double hi = centerYuan * (1 + ratio);
        p.minPrice = yuanToFen(lo);
        p.maxPrice = yuanToFen(hi);
    }

    private static int yuanToFen(double yuan) {
        return (int) Math.round(yuan * 100.0);
    }

    public static class Parsed {
        /** 传给 ES 的检索词，保留原句利于宽召回 */
        private String searchKey = "";
        private String category;
        private String brand;
        private Integer minPrice;
        private Integer maxPrice;
        /** 对应 ES specColor，可选精确过滤 */
        private String specColor;
        /** 对应 ES specSize，可选精确过滤 */
        private String specSize;

        public String getSearchKey() {
            return searchKey;
        }

        public void setSearchKey(String searchKey) {
            this.searchKey = searchKey == null ? "" : searchKey;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getBrand() {
            return brand;
        }

        public void setBrand(String brand) {
            this.brand = brand;
        }

        public Integer getMinPrice() {
            return minPrice;
        }

        public void setMinPrice(Integer minPrice) {
            this.minPrice = minPrice;
        }

        public Integer getMaxPrice() {
            return maxPrice;
        }

        public void setMaxPrice(Integer maxPrice) {
            this.maxPrice = maxPrice;
        }

        public boolean hasPriceBand() {
            return minPrice != null && maxPrice != null;
        }

        public String getSpecColor() {
            return specColor;
        }

        public void setSpecColor(String specColor) {
            this.specColor = specColor;
        }

        public String getSpecSize() {
            return specSize;
        }

        public void setSpecSize(String specSize) {
            this.specSize = specSize;
        }
    }
}
