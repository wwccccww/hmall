package com.hmall.item.domain.query;

import com.hmall.common.domain.PageQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(description = "商品分页查询条件")
public class ItemPageQuery extends PageQuery {
    @ApiModelProperty("搜索关键字")
    private String key;
    @ApiModelProperty("商品分类")
    private String category;
    @ApiModelProperty("商品品牌")
    private String brand;
    @ApiModelProperty("价格最小值")
    private Integer minPrice;
    @ApiModelProperty("价格最大值")
    private Integer maxPrice;
    @ApiModelProperty("规格颜色（与 ES specColor 文本匹配，可选）")
    private String specColor;
    @ApiModelProperty("规格尺寸（与 ES specSize 文本匹配，可选）")
    private String specSize;
    /**
     * 商品状态：1 上架，2 下架，3 删除。不传则仅查上架（1），与同步到 ES 的数据一致。
     */
    @ApiModelProperty("商品状态，不传默认 1（仅上架）")
    private Integer status;
}
