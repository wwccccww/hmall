package com.hmall.ai.service.impl;

import com.hmall.ai.llm.LlmClient;
import com.hmall.ai.web.dto.ChatRequest;
import com.hmall.ai.web.dto.ChatResponse;
import com.hmall.ai.service.AiChatService;
import com.hmall.ai.service.rag.SimpleRagService;
import com.hmall.ai.tool.UserTools;
import com.hmall.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private final LlmClient llmClient;
    private final SimpleRagService ragService;
    private final UserTools userTools;

    private static final Pattern ORDER_ID = Pattern.compile("(\\d{6,})");

    @Override
    public ChatResponse chatSync(ChatRequest request) {
        var tool = tryToolCall(request.getMessage());
        if (tool != null) {
            return tool;
        }
        var rag = ragService.buildRagPrompt(request.getMessage());
        String answer = llmClient.chat(rag.prompt());
        return ChatResponse.builder()
                .answer(answer)
                .sources(rag.sources())
                .actions(List.of())
                .build();
    }

    @Override
    public SseEmitter chatStream(ChatRequest request) {
        SseEmitter emitter = new SseEmitter(Duration.ofMinutes(2).toMillis());
        ChatResponse tool = tryToolCall(request.getMessage());
        if (tool != null) {
            try {
                emitter.send(SseEmitter.event().name("result").data(tool));
                emitter.send(SseEmitter.event().name("done").data(Map.of()));
            } catch (Exception e) {
                emitter.completeWithError(e);
                return emitter;
            }
            emitter.complete();
            return emitter;
        }
        var rag = ragService.buildRagPrompt(request.getMessage());
        llmClient.streamChat(rag.prompt())
                .subscribe(
                        chunk -> {
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("message")
                                        .data(Map.of("delta", chunk)));
                            } catch (IOException e) {
                                log.warn("SSE send failed", e);
                                emitter.completeWithError(e);
                            }
                        },
                        err -> {
                            try {
                                emitter.send(SseEmitter.event().name("error").data(Map.of("message", err.getMessage())));
                            } catch (Exception ignored) {
                            }
                            emitter.completeWithError(err);
                        },
                        () -> {
                            try {
                                emitter.send(SseEmitter.event().name("sources").data(Map.of("sources", rag.sources())));
                                emitter.send(SseEmitter.event().name("done").data(Map.of()));
                            } catch (Exception ignored) {
                            }
                            emitter.complete();
                        }
                );
        return emitter;
    }

    private ChatResponse tryToolCall(String message) {
        if (message == null) return null;
        String m = message.trim();
        Long userId = UserContext.getUser();
        boolean loggedIn = userId != null;

        List<Map<String, Object>> actions = new ArrayList<>();
        List<Map<String, Object>> sources = new ArrayList<>();

        if (m.contains("我的优惠券") || m.contains("可用券") || m.contains("优惠券有哪些")) {
            if (!loggedIn) {
                return ChatResponse.builder().answer("请先登录后再查询“我的优惠券”。").sources(List.of()).actions(List.of()).build();
            }
            actions.add(Map.of("tool", "queryMyCoupons"));
            List<Map<String, Object>> coupons = userTools.queryMyCoupons();
            sources.add(Map.of("type", "coupon_list", "count", coupons.size()));
            return ChatResponse.builder()
                    .answer("已为你查询到优惠券列表（见 sources）。你也可以告诉我你想买的商品/预算，我帮你挑最划算的券。")
                    .sources(List.of(Map.of("type", "coupons", "data", coupons)))
                    .actions(actions)
                    .build();
        }

        if (m.contains("我的地址") || m.contains("收货地址")) {
            if (!loggedIn) {
                return ChatResponse.builder().answer("请先登录后再查询“我的地址”。").sources(List.of()).actions(List.of()).build();
            }
            actions.add(Map.of("tool", "queryMyAddresses"));
            List<Map<String, Object>> addresses = userTools.queryMyAddresses();
            return ChatResponse.builder()
                    .answer("已为你查询到地址列表（已脱敏，见 sources）。")
                    .sources(List.of(Map.of("type", "addresses", "data", addresses)))
                    .actions(actions)
                    .build();
        }

        if (m.contains("我的信息") || m.contains("我是谁") || m.contains("我的账号")) {
            if (!loggedIn) {
                return ChatResponse.builder().answer("请先登录后再查询“我的信息”。").sources(List.of()).actions(List.of()).build();
            }
            actions.add(Map.of("tool", "queryMe"));
            Map<String, Object> me = userTools.queryMe();
            return ChatResponse.builder()
                    .answer("已为你查询到当前登录信息（见 sources）。")
                    .sources(List.of(Map.of("type", "me", "data", me)))
                    .actions(actions)
                    .build();
        }

        if (m.contains("订单") && (m.contains("状态") || m.contains("查询") || m.contains("进度"))) {
            if (!loggedIn) {
                return ChatResponse.builder().answer("请先登录后再查询订单。").sources(List.of()).actions(List.of()).build();
            }
            Matcher matcher = ORDER_ID.matcher(m);
            if (!matcher.find()) {
                return ChatResponse.builder()
                        .answer("请把订单号发我（例如：123456789），我才能帮你查询订单状态。")
                        .sources(List.of())
                        .actions(List.of(Map.of("need", "orderId")))
                        .build();
            }
            Long orderId = Long.valueOf(matcher.group(1));
            actions.add(Map.of("tool", "queryOrderById", "orderId", orderId));
            Map<String, Object> order = userTools.queryOrderById(orderId);
            return ChatResponse.builder()
                    .answer("已为你查询订单信息（见 sources）。如需我帮你解读字段含义，也可以继续问我。")
                    .sources(List.of(Map.of("type", "order", "data", order)))
                    .actions(actions)
                    .build();
        }

        return null;
    }
}

