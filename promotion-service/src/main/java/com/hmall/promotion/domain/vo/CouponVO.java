package com.hmall.promotion.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(description = "优惠券视图对象")
public class CouponVO {

    @ApiModelProperty("优惠券ID")
    private Long id;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("优惠类型：1=满减 2=折扣")
    private Integer type;

    @ApiModelProperty("优惠值")
    private Integer discountValue;

    @ApiModelProperty("使用门槛（分）")
    private Integer threshold;

    @ApiModelProperty("剩余库存")
    private Integer stock;

    @ApiModelProperty("活动开始时间")
    private LocalDateTime beginTime;

    @ApiModelProperty("活动结束时间")
    private LocalDateTime endTime;

    @ApiModelProperty("状态：1=草稿 2=进行中 3=已结束 4=暂停")
    private Integer status;
}
