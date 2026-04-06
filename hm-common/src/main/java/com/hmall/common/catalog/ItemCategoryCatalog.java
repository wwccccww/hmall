package com.hmall.common.catalog;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 与库表/ES {@code category} 一致的类目白名单及口语同义词归一（item 检索与导购意图共用）。
 */
public final class ItemCategoryCatalog {

    /** 与 {@code hm-item.item} 类目列一致的合法取值（顺序固定便于 prompt 引用）。 */
    public static final List<String> CANONICAL_CATEGORIES = List.of(
            "休闲鞋",
            "手机",
            "拉拉裤",
            "拉杆箱",
            "曲面电视",
            "牛仔裤",
            "牛奶",
            "真皮包",
            "硬盘",
            "老花镜");

    private static final Set<String> CANONICAL_SET = Set.copyOf(CANONICAL_CATEGORIES);

    private static final Map<String, String> ALIAS_TO_CANONICAL = new HashMap<>();

    private static final List<PhraseHit> PHRASE_HITS = new ArrayList<>();

    /** 与导购解析一致，最长先匹配 */
    private static final List<String> COLOR_HINTS_LONGEST_FIRST = List.of(
            "深蓝色", "浅蓝色", "天蓝色", "藏青色", "象牙白", "香槟金", "玫瑰金",
            "黑色", "白色", "红色", "蓝色", "绿色", "黄色", "紫色", "灰色", "粉色", "棕色", "橙色", "金色", "银色", "米色",
            "深蓝", "浅蓝", "墨绿", "深灰",
            "黑", "白", "红", "蓝", "绿", "黄", "紫", "灰", "粉", "棕", "橙");

    static {
        for (String c : CANONICAL_CATEGORIES) {
            ALIAS_TO_CANONICAL.put(c, c);
        }
        putAlias("运动鞋", "休闲鞋");
        putAlias("跑步鞋", "休闲鞋");
        putAlias("跑鞋", "休闲鞋");
        putAlias("篮球鞋", "休闲鞋");
        putAlias("足球鞋", "休闲鞋");
        putAlias("徒步鞋", "休闲鞋");
        putAlias("登山鞋", "休闲鞋");
        putAlias("帆布鞋", "休闲鞋");
        putAlias("板鞋", "休闲鞋");
        putAlias("网面鞋", "休闲鞋");
        putAlias("飞织鞋", "休闲鞋");
        putAlias("凉鞋", "休闲鞋");
        putAlias("拖鞋", "休闲鞋");
        putAlias("女鞋", "休闲鞋");
        putAlias("男鞋", "休闲鞋");
        putAlias("单鞋", "休闲鞋");
        putAlias("马丁靴", "休闲鞋");
        putAlias("短靴", "休闲鞋");
        putAlias("靴子", "休闲鞋");
        putAlias("鞋子", "休闲鞋");
        putAlias("鞋", "休闲鞋");
        putAlias("iphone", "手机");
        putAlias("电话", "手机");
        putAlias("智能手机", "手机");
        putAlias("纸尿裤", "拉拉裤");
        putAlias("尿不湿", "拉拉裤");
        putAlias("成长裤", "拉拉裤");
        putAlias("行李箱", "拉杆箱");
        putAlias("旅行箱", "拉杆箱");
        putAlias("登机箱", "拉杆箱");
        putAlias("托运箱", "拉杆箱");
        putAlias("电视", "曲面电视");
        putAlias("彩电", "曲面电视");
        putAlias("智能电视", "曲面电视");
        putAlias("牛仔库", "牛仔裤");
        putAlias("仔裤", "牛仔裤");
        putAlias("纯牛奶", "牛奶");
        putAlias("鲜奶", "牛奶");
        putAlias("酸奶", "牛奶");
        putAlias("奶制品", "牛奶");
        putAlias("皮包", "真皮包");
        putAlias("女士包", "真皮包");
        putAlias("男包", "真皮包");
        putAlias("手提包", "真皮包");
        putAlias("斜挎包", "真皮包");
        putAlias("固态硬盘", "硬盘");
        putAlias("机械硬盘", "硬盘");
        putAlias("移动硬盘", "硬盘");
        putAlias("SSD", "硬盘");
        putAlias("ssd", "硬盘");
        putAlias("老人镜", "老花镜");
        putAlias("阅读镜", "老花镜");
        putAlias("阅读眼镜", "老花镜");

        LinkedHashSet<String> dedup = new LinkedHashSet<>();
        for (Map.Entry<String, String> e : ALIAS_TO_CANONICAL.entrySet()) {
            dedup.add(e.getKey() + "\0" + e.getValue());
        }
        for (String c : CANONICAL_CATEGORIES) {
            dedup.add(c + "\0" + c);
        }
        for (String s : dedup) {
            int i = s.indexOf('\0');
            PHRASE_HITS.add(new PhraseHit(s.substring(0, i), s.substring(i + 1)));
        }
        PHRASE_HITS.sort(Comparator.comparingInt((PhraseHit h) -> h.phrase.length()).reversed());
    }

    private static void putAlias(String alias, String canonical) {
        ALIAS_TO_CANONICAL.put(alias, canonical);
    }

    private static final class PhraseHit {
        final String phrase;
        final String category;

        PhraseHit(String phrase, String category) {
            this.phrase = phrase;
            this.category = category;
        }
    }

    private ItemCategoryCatalog() {
    }

    /** 从用户检索词中抽取颜色片段（用于 ES should 加权，不过滤）。 */
    public static String inferColorKeyword(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        for (String hint : COLOR_HINTS_LONGEST_FIRST) {
            if (t.contains(hint)) {
                return hint;
            }
        }
        return null;
    }

    public static String normalizeLlmCategory(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String t = raw.trim();
        if (CANONICAL_SET.contains(t)) {
            return t;
        }
        String mapped = ALIAS_TO_CANONICAL.get(t);
        if (mapped != null) {
            return mapped;
        }
        String compact = t.replaceAll("\\s+", "");
        mapped = ALIAS_TO_CANONICAL.get(compact);
        if (mapped != null) {
            return mapped;
        }
        mapped = ALIAS_TO_CANONICAL.get(t.toLowerCase());
        return mapped;
    }

    public static String inferCategoryFromUserText(String userText) {
        if (userText == null || userText.isBlank()) {
            return null;
        }
        String text = userText.trim();
        for (PhraseHit hit : PHRASE_HITS) {
            if (!matchesPhrase(text, hit.phrase)) {
                continue;
            }
            if (requiresPhoneProduct(text, hit)) {
                if (!hasPhoneProductKeyword(text)) {
                    continue;
                }
            }
            if (requiresShoeProduct(text, hit)) {
                if (!hasShoeProductKeyword(text)) {
                    continue;
                }
            }
            return hit.category;
        }
        return null;
    }

    private static boolean matchesPhrase(String text, String phrase) {
        if (text.contains(phrase)) {
            return true;
        }
        boolean ascii = phrase.chars().allMatch(ch -> ch < 128);
        return ascii && text.toLowerCase().contains(phrase.toLowerCase());
    }

    private static boolean requiresPhoneProduct(String text, PhraseHit hit) {
        return "手机".equals(hit.phrase) && "手机".equals(hit.category);
    }

    private static boolean requiresShoeProduct(String text, PhraseHit hit) {
        if (!"休闲鞋".equals(hit.category)) {
            return false;
        }
        return hit.phrase.length() <= 2 && ("鞋".equals(hit.phrase) || "靴".equals(hit.phrase));
    }

    private static boolean hasShoeProductKeyword(String key) {
        if (!key.contains("鞋") && !key.contains("靴")) {
            return false;
        }
        if (key.contains("鞋")) {
            String[] exclude = {"鞋柜", "鞋架", "鞋垫", "鞋带", "鞋油", "鞋盒", "鞋刷", "鞋袜", "鞋拔"};
            for (String ex : exclude) {
                if (key.contains(ex)) {
                    return false;
                }
            }
            return true;
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

    public static String canonicalListForPrompt() {
        return String.join("、", CANONICAL_CATEGORIES);
    }
}
