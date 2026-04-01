package com.hmall.ai.service;

import com.hmall.ai.web.dto.ChatRequest;
import com.hmall.ai.web.dto.ChatResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AiChatService {
    ChatResponse chatSync(ChatRequest request);

    SseEmitter chatStream(ChatRequest request);
}

