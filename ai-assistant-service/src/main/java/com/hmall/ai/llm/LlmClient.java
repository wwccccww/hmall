package com.hmall.ai.llm;

import reactor.core.publisher.Flux;

public interface LlmClient {
    String chat(String userMessage);

    Flux<String> streamChat(String userMessage);

    /**
     * 自定义 system，用于意图抽取等；temperature 由调用方指定（如 0.1）。
     */
    String chatWithSystem(String systemContent, String userContent, double temperature);
}

