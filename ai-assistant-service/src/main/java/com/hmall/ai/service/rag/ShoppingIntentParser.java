package com.hmall.ai.service.rag;

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

        // 类目 term 过滤要求 ES 与 DB 完全一致，这里不轻易填 category，交给 key 宽召回 + ES 内 wildcard
        return p;
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
    }
}
