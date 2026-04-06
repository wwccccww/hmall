package com.hmall.ai.web.dto;

import com.hmall.ai.service.rag.ShoppingIntentParser;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "ES /search/list 对齐的检索意图（分解结果）")
public class SearchIntentResponse {

    @ApiModelProperty("对应查询参数 key，宽召回用，通常为用户原句")
    private String key;

    @ApiModelProperty("品牌（有则传入 search/list 的 brand）")
    private String brand;

    @ApiModelProperty("类目（有则传入 search/list 的 category）")
    private String category;

    @ApiModelProperty("最低价（分），与 maxPrice 成对出现")
    private Integer minPrice;

    @ApiModelProperty("最高价（分）")
    private Integer maxPrice;

    @ApiModelProperty("是否已形成价格区间（min/max 均有）")
    private boolean hasPriceBand;

    @ApiModelProperty("解析器标识，便于排障与后续切换策略")
    private String parser;

    public static SearchIntentResponse fromParsed(ShoppingIntentParser.Parsed parsed) {
        if (parsed == null) {
            return SearchIntentResponse.builder()
                    .key("")
                    .hasPriceBand(false)
                    .parser("rule_based")
                    .build();
        }
        return SearchIntentResponse.builder()
                .key(parsed.getSearchKey())
                .brand(parsed.getBrand())
                .category(parsed.getCategory())
                .minPrice(parsed.getMinPrice())
                .maxPrice(parsed.getMaxPrice())
                .hasPriceBand(parsed.hasPriceBand())
                .parser("rule_based")
                .build();
    }
}
