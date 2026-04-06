package com.hmall.ai.web.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@ApiModel(description = "购物意图解析请求")
public class IntentParseRequest {
    @ApiModelProperty(value = "用户原始问题", required = true, example = "推荐 2000 元左右华为手机")
    @NotBlank
    @Size(max = 500)
    private String message;
}
