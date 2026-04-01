package com.hmall.ai.web;

import com.hmall.ai.service.AiChatService;
import com.hmall.ai.web.dto.ChatRequest;
import com.hmall.ai.web.dto.ChatResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.validation.Valid;

@Api(tags = "AI 导购助手")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    @ApiOperation("对话（同步）")
    @PostMapping("/chat/sync")
    public ChatResponse chatSync(@Valid @RequestBody ChatRequest request) {
        return aiChatService.chatSync(request);
    }

    @ApiOperation("对话（SSE 流式）")
    @PostMapping("/chat")
    public SseEmitter chatStream(@Valid @RequestBody ChatRequest request) {
        return aiChatService.chatStream(request);
    }
}

