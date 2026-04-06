package com.hmall.ai.service.rag;

import lombok.Data;

/**
 * LLM 抽取的购物检索槽位（单位：分）。
 */
@Data
public class ShoppingIntentLlmFields {
    private String searchKey;
    private Integer minPrice;
    private Integer maxPrice;
    /** 商品类目自然语言，仅并入全文 key，不做 ES category term（避免与索引枚举不一致） */
    private String category;
    private String brand;
    /** 颜色，写入 specColor 过滤并入 key */
    private String color;
    /** 尺寸，写入 specSize 过滤并入 key */
    private String size;
}
