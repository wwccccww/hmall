package com.hmall.ai.service.rag;

import com.hmall.common.catalog.ItemCategoryCatalog;
import org.springframework.stereotype.Component;

/**
 * 将规则 {@link ShoppingIntentParser.Parsed} 与 LLM {@link ShoppingIntentLlmFields} 合并。
 * <p>
 * 优先级：正则解出的价格带优先于 LLM；品牌以规则非空优先；
 * {@code category} 仅允许 {@link ItemCategoryCatalog} 白名单值：LLM 归一化优先，否则保留规则推断并再归一。
 */
@Component
public class ShoppingIntentMergeService {

    public ShoppingIntentParser.Parsed merge(ShoppingIntentParser.Parsed rule, ShoppingIntentLlmFields llm) {
        ShoppingIntentParser.Parsed out = copy(rule);
        if (llm == null) {
            applyCanonicalCategory(out, null);
            return out;
        }
        if (!isBlank(llm.getBrand()) && isBlank(out.getBrand())) {
            out.setBrand(trim(llm.getBrand()));
        }
        if (!rule.hasPriceBand() && llm.getMinPrice() != null && llm.getMaxPrice() != null) {
            out.setMinPrice(llm.getMinPrice());
            out.setMaxPrice(llm.getMaxPrice());
        }
        if (!isBlank(llm.getColor())) {
            out.setSpecColor(trim(llm.getColor()));
        }
        if (!isBlank(llm.getSize())) {
            out.setSpecSize(trim(llm.getSize()));
        }
        out.setSearchKey(mergeSearchKey(out.getSearchKey(), llm));
        applyCanonicalCategory(out, llm);
        return out;
    }

    /** LLM 的 category 经白名单归一后优先；否则用已有 category 再归一；再无则从合并后的 key 推断。 */
    private static void applyCanonicalCategory(ShoppingIntentParser.Parsed out, ShoppingIntentLlmFields llm) {
        String cat = null;
        if (llm != null && !isBlank(llm.getCategory())) {
            cat = ItemCategoryCatalog.normalizeLlmCategory(llm.getCategory());
        }
        if (cat == null && !isBlank(out.getCategory())) {
            cat = ItemCategoryCatalog.normalizeLlmCategory(out.getCategory());
        }
        if (cat == null) {
            cat = ItemCategoryCatalog.inferCategoryFromUserText(out.getSearchKey());
        }
        if (cat != null) {
            out.setCategory(cat);
        }
    }

    private static String mergeSearchKey(String ruleKey, ShoppingIntentLlmFields llm) {
        StringBuilder sb = new StringBuilder(ruleKey == null ? "" : ruleKey.trim());
        appendIfMissing(sb, llm.getSearchKey());
        appendIfMissing(sb, llm.getCategory());
        appendIfMissing(sb, llm.getColor());
        return sb.toString().trim();
    }

    private static void appendIfMissing(StringBuilder sb, String part) {
        if (isBlank(part)) {
            return;
        }
        String p = trim(part);
        if (sb.indexOf(p) >= 0) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(' ');
        }
        sb.append(p);
    }

    private static ShoppingIntentParser.Parsed copy(ShoppingIntentParser.Parsed r) {
        ShoppingIntentParser.Parsed p = new ShoppingIntentParser.Parsed();
        p.setSearchKey(r.getSearchKey());
        p.setCategory(r.getCategory());
        p.setBrand(r.getBrand());
        p.setMinPrice(r.getMinPrice());
        p.setMaxPrice(r.getMaxPrice());
        p.setSpecColor(r.getSpecColor());
        p.setSpecSize(r.getSpecSize());
        return p;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
