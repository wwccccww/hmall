package com.hmall.ai.web.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "AI 对话响应")
public class ChatResponse {
    @ApiModelProperty("回答文本")
    private String answer;

    @ApiModelProperty("引用来源（RAG/工具调用）")
    private List<Map<String, Object>> sources;

    @ApiModelProperty("工具调用动作（可选，便于前端渲染）")
    private List<Map<String, Object>> actions;
}

