package com.hmall.ai.service.rag;

import org.springframework.stereotype.Service;

/**
 * 购物检索意图解析（规则版），供 HTTP 暴露与 RAG 查询共用同一实现。
 */
@Service
public class ShoppingIntentParseService {

    /**
     * 将用户自然语言解析为 {@link ShoppingIntentParser.Parsed}，
     * 字段与 item-service {@code GET /search/list} 的查询参数对齐。
     */
    public ShoppingIntentParser.Parsed parse(String userMessage) {
        return ShoppingIntentParser.parse(userMessage);
    }
}
