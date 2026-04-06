package com.hmall.ai.service;

import com.hmall.ai.web.dto.ChatRequest;
import com.hmall.ai.web.dto.ChatResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AiChatService {
    /** 同步调用 */
    ChatResponse chatSync(ChatRequest request);

    /** 流式调用 */
    SseEmitter chatStream(ChatRequest request);
}

