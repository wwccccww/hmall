package com.hmall.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmall.ai.llm.BailianResponsesClient;
import com.hmall.ai.llm.LlmClient;
import com.hmall.ai.llm.LlmProperties;
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
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private final LlmClient llmClient;
    private final LlmProperties llmProperties;
    private final BailianResponsesClient bailianResponsesClient;
    private final ObjectMapper objectMapper;
    private final SimpleRagService ragService;
    private final UserTools userTools;

    private static final Pattern ORDER_ID = Pattern.compile("(\\d{6,})");

    private static final String BAILIAN_INPUT_PREFIX =
            "你是电商导购助手，请用中文、结合知识库（如有）简洁回答；勿编造知识库未提供的事实。\n\n用户问题：\n";

    @Override
    public ChatResponse chatSync(ChatRequest request) {
        var tool = tryToolCall(request.getMessage());
        if (tool != null) {
            return tool;
        }
        GeneralAnswer ga = resolveGeneralAnswer(request.getMessage());
        return ChatResponse.builder()
                .answer(ga.answer)
                .sources(ga.sources)
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
                emitter.send(SseEmitter.event().name("done").data((Object) Map.of("ok", true)));
            } catch (Exception e) {
                emitter.completeWithError(e);
                return emitter;
            }
            emitter.complete();
            return emitter;
        }
        CompletableFuture.runAsync(() -> {
            try {
                GeneralAnswer ga = resolveGeneralAnswer(request.getMessage());
                String answer = ga.answer;
                if (answer == null) {
                    answer = "";
                }
                for (String chunk : splitToChunks(answer, 40)) {
                    emitter.send(SseEmitter.event().name("message").data((Object) Map.of("delta", chunk)));
                }
                emitter.send(SseEmitter.event().name("sources").data((Object) Map.of("sources", ga.sources)));
                emitter.send(SseEmitter.event().name("done").data((Object) Map.of("ok", true)));
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data((Object) Map.of("message", String.valueOf(e.getMessage()))));
                } catch (IOException ignored) {
                }
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    private List<String> splitToChunks(String text, int chunkSize) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        int n = Math.max(1, chunkSize);
        int len = text.length();
        ArrayList<String> out = new ArrayList<>((len + n - 1) / n);
        for (int i = 0; i < len; i += n) {
            out.add(text.substring(i, Math.min(len, i + n)));
        }
        return out;
    }

    /**
     * 通用导购：若配置了百炼应用 ID，则优先走 Responses API（控制台可绑知识库，内置 RAG）；失败或空结果时回退本地 SimpleRag + Chat Completions。
     */
    private GeneralAnswer resolveGeneralAnswer(String message) {
        if (bailianResponsesClient.enabled()) {
            try {
                String answer = bailianResponsesClient.complete(BAILIAN_INPUT_PREFIX + message);
                if (answer != null && !answer.isBlank()) {
                    return new GeneralAnswer(answer, bailianSourceMeta());
                }
            } catch (Exception e) {
                log.warn("Bailian Responses 调用失败，回退本地 RAG: {}", e.getMessage());
            }
        }
        var rag = ragService.buildRagPrompt(message);
        String answer = llmClient.chat(rag.prompt());
        return new GeneralAnswer(answer == null ? "" : answer, rag.sources());
    }

    private List<Map<String, Object>> bailianSourceMeta() {
        return List.of(Map.of(
                "type", "bailian_app",
                "appId", String.valueOf(llmProperties.getBailianAppId()),
                "note", "回答由百炼智能体生成；知识库检索在百炼侧完成（见控制台应用配置）"
        ));
    }

    private static final class GeneralAnswer {
        final String answer;
        final List<Map<String, Object>> sources;

        GeneralAnswer(String answer, List<Map<String, Object>> sources) {
            this.answer = answer;
            this.sources = sources;
        }
    }

    /**
     * 工具路径：先查下游，再在配置了 API Key 时用 LLM 将结构化结果合成自然语言。
     */
    private ChatResponse tryToolCall(String message) {
        if (message == null) {
            return null;
        }
        String m = message.trim();
        Long userId = UserContext.getUser();
        boolean loggedIn = userId != null;

        List<Map<String, Object>> actions = new ArrayList<>();

        if (m.contains("我的优惠券") || m.contains("可用券") || m.contains("优惠券有哪些")) {
            if (!loggedIn) {
                return ChatResponse.builder().answer("请先登录后再查询“我的优惠券”。").sources(List.of()).actions(List.of()).build();
            }
            actions.add(Map.of("tool", "queryMyCoupons"));
            List<Map<String, Object>> coupons = userTools.queryMyCoupons();
            List<Map<String, Object>> sources = List.of(Map.of("type", "coupons", "data", coupons));
            String fallback = "已为你查询到优惠券列表（见 sources）。你也可以告诉我你想买的商品/预算，我帮你挑最划算的券。";
            String answer = synthesizeToolAnswer(message, sources, fallback);
            return ChatResponse.builder()
                    .answer(answer)
                    .sources(sources)
                    .actions(actions)
                    .build();
        }

        if (m.contains("我的地址") || m.contains("收货地址")) {
            if (!loggedIn) {
                return ChatResponse.builder().answer("请先登录后再查询“我的地址”。").sources(List.of()).actions(List.of()).build();
            }
            actions.add(Map.of("tool", "queryMyAddresses"));
            List<Map<String, Object>> addresses = userTools.queryMyAddresses();
            List<Map<String, Object>> sources = List.of(Map.of("type", "addresses", "data", addresses));
            String fallback = "已为你查询到地址列表（已脱敏，见 sources）。";
            String answer = synthesizeToolAnswer(message, sources, fallback);
            return ChatResponse.builder()
                    .answer(answer)
                    .sources(sources)
                    .actions(actions)
                    .build();
        }

        if (m.contains("我的信息") || m.contains("我是谁") || m.contains("我的账号")) {
            if (!loggedIn) {
                return ChatResponse.builder().answer("请先登录后再查询“我的信息”。").sources(List.of()).actions(List.of()).build();
            }
            actions.add(Map.of("tool", "queryMe"));
            Map<String, Object> me = userTools.queryMe();
            List<Map<String, Object>> sources = List.of(Map.of("type", "me", "data", me));
            String fallback = "已为你查询到当前登录信息（见 sources）。";
            String answer = synthesizeToolAnswer(message, sources, fallback);
            return ChatResponse.builder()
                    .answer(answer)
                    .sources(sources)
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
            List<Map<String, Object>> sources = List.of(Map.of("type", "order", "data", order));
            String fallback = "已为你查询订单信息（见 sources）。如需我帮你解读字段含义，也可以继续问我。";
            String answer = synthesizeToolAnswer(message, sources, fallback);
            return ChatResponse.builder()
                    .answer(answer)
                    .sources(sources)
                    .actions(actions)
                    .build();
        }

        return null;
    }

    private String synthesizeToolAnswer(String userMessage, List<Map<String, Object>> sources, String fallback) {
        String key = llmProperties.getApiKey();
        if (key == null || key.isBlank()) {
            return fallback;
        }
        try {
            String json = objectMapper.writeValueAsString(sources);
            String prompt = "用户问题：\n" + userMessage + "\n\n系统查询结果（JSON，仅可据此回答，勿编造字段）：\n"
                    + json + "\n\n请用简洁中文直接回答；列表请做可读摘要；已脱敏字段（如 ***）请保持脱敏表述。";
            String out = llmClient.chat(prompt);
            if (out != null && !out.isBlank()) {
                return out;
            }
        } catch (Exception e) {
            log.warn("tool answer synthesis failed: {}", e.getMessage());
        }
        return fallback;
    }
}
