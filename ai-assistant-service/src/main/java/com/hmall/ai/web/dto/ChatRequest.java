package com.hmall.ai.web.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.Map;

@Data
@ApiModel(description = "AI 对话请求")
public class ChatRequest {
    @ApiModelProperty(value = "用户消息", required = true, example = "我想买一台性价比高的手机，预算 2000 左右")
    @NotBlank
    @Size(max = 500)
    private String message;

    @ApiModelProperty(value = "可选：上下文（前端可自行维护）")
    private Map<String, Object> context;
}

