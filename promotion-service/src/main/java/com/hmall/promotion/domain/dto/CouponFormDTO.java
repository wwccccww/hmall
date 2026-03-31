package com.hmall.promotion.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.time.LocalDateTime;

@Data
@ApiModel(description = "优惠券创建/修改表单")
public class CouponFormDTO {

    @ApiModelProperty("优惠券名称")
    @NotBlank(message = "优惠券名称不能为空")
    private String name;

    @ApiModelProperty("优惠类型：1=满减 2=折扣")
    @NotNull(message = "优惠类型不能为空")
    private Integer type;

    @ApiModelProperty("优惠值（满减金额/分，折扣时为百分比如85代表85折）")
    @NotNull(message = "优惠值不能为空")
    @Min(value = 1, message = "优惠值必须大于0")
    private Integer discountValue;

    @ApiModelProperty("使用门槛（分），0=无门槛")
    private Integer threshold = 0;

    @ApiModelProperty("发行总量")
    @NotNull(message = "发行总量不能为空")
    @Min(value = 1, message = "发行总量至少为1")
    private Integer publishCount;

    @ApiModelProperty("活动开始时间")
    @NotNull(message = "活动开始时间不能为空")
    private LocalDateTime beginTime;

    @ApiModelProperty("活动结束时间")
    @NotNull(message = "活动结束时间不能为空")
    private LocalDateTime endTime;

    @ApiModelProperty("适用范围：1=全场 3=指定类目")
    private Integer scopeType = 1;

    @ApiModelProperty("指定类目名称列表（当 scopeType=3 时必填，需与 item.category 完全一致）")
    private List<String> categoryNames;

    @ApiModelProperty("指定品牌名称列表（当 scopeType=2 时必填，需与 item.brand 完全一致）")
    private List<String> brandNames;

    @ApiModelProperty("指定商家ID列表（当 scopeType=4 时必填）")
    private List<Long> shopIds;
}
