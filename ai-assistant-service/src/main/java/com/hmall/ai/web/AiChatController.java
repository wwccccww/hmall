package com.hmall.ai.web;

import com.hmall.ai.service.AiChatService;
import com.hmall.ai.service.rag.ShoppingIntentParseService;
import com.hmall.ai.web.dto.ChatRequest;
import com.hmall.ai.web.dto.ChatResponse;
import com.hmall.ai.web.dto.IntentParseRequest;
import com.hmall.ai.web.dto.SearchIntentResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.validation.Valid;

@Api(tags = "AI 导购助手")
@RestController
@RequestMapping("/ai")
public class AiChatController {

    private final AiChatService aiChatService;
    private final ShoppingIntentParseService shoppingIntentParseService;

    public AiChatController(
            AiChatService aiChatService,
            ShoppingIntentParseService shoppingIntentParseService) {
        this.aiChatService = aiChatService;
        this.shoppingIntentParseService = shoppingIntentParseService;
    }

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

    @ApiOperation("购物意图分解（供 ES /search/list 等检索使用，与 RAG 内部逻辑一致）")
    @PostMapping("/intent/parse")
    public SearchIntentResponse parseSearchIntent(@Valid @RequestBody IntentParseRequest intentRequest) {
        return SearchIntentResponse.fromParsed(
                shoppingIntentParseService.parse(intentRequest.getMessage()));
    }
}
