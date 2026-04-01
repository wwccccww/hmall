package com.hmall.ai.llm;

import reactor.core.publisher.Flux;

public interface LlmClient {
    String chat(String userMessage);

    Flux<String> streamChat(String userMessage);
}

