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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    /** LLM 客户端 */
    private final LlmClient llmClient;
    /** LLM 配置 */
    private final LlmProperties llmProperties;
    /** 百炼智能体 Responses 客户端 */
    private final BailianResponsesClient bailianResponsesClient;
    /** JSON 序列化器 */
    private final ObjectMapper objectMapper;
    /** RAG 服务 */
    private final SimpleRagService ragService;
    /** 用户工具 */
    private final UserTools userTools;
    /** 订单ID正则表达式 */
    private static final Pattern ORDER_ID = Pattern.compile("(\\d{6,})");
    /** 百炼智能体输入前缀 */
    private static final String BAILIAN_INPUT_PREFIX =
            "你是电商导购助手，请用中文、结合知识库（如有）简洁回答；勿编造知识库未提供的事实。\n\n用户问题：\n";

    /**
     * 同步调用
     * @param request 调用请求
     * @return 调用响应
     */
    //TODO 可能做优化，比如缓存通用回答，或者异步调用
    @Override
    public ChatResponse chatSync(ChatRequest request) {
        // 先尝试工具调用
        var tool = tryToolCall(request.getMessage());
        if (tool != null) {
            return tool;
        }
        // 再尝试一般回答
        GeneralAnswer ga = resolveGeneralAnswer(request.getMessage());
        return ChatResponse.builder()
                .answer(ga.answer)
                .sources(ga.sources)
                .actions(buildShoppingActions(ga.sources))
                .build();
    }



     /**
     * 流式调用
      * <p>
      * 该方法实现了基于Server-Sent Events (SSE)的流式聊天功能，支持以下特性：
      * 1. 优先尝试工具调用（如购物相关操作）
      * 2. 工具调用失败时回退到通用回答
      * 3. 将回答内容分块流式发送给客户端
      * 4. 发送来源信息和购物操作建议
      * 5. 处理异常情况并发送错误信息
      * </p>
      *
      * @param request 聊天请求对象，包含用户消息内容
      * @return SseEmitter实例，用于向客户端流式推送数据
     */
    @Override
    public SseEmitter chatStream(ChatRequest request) {
        SseEmitter emitter = new SseEmitter(Duration.ofMinutes(2).toMillis());
        ChatResponse tool = tryToolCall(request.getMessage());
        if (tool != null) {
            try {
                // 工具调用成功，直接返回结果
                emitter.send(SseEmitter.event().name("result").data(tool));
                emitter.send(SseEmitter.event().name("done").data((Object) Map.of("ok", true)));
            } catch (Exception e) {
                emitter.completeWithError(e);
                return emitter;
            }
            emitter.complete();
            return emitter;
        }
        // 工具调用失败，回退到通用回答
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
                // 发送来源信息和购物操作建议
                Map<String, Object> meta = new HashMap<>();
                meta.put("sources", ga.sources);
                meta.put("actions", buildShoppingActions(ga.sources));
                emitter.send(SseEmitter.event().name("sources").data((Object) meta));
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

    /**
     * 将文本分割成指定大小的块，用于流式返回
     * @param text 要分割的文本
     * @param chunkSize 每块的大小
     * @return 分割后的文本块列表
     */
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
     * 通用导购：若配置了百炼应用 ID，则优先走 Responses API（控制台可绑知识库，内置 RAG）；
     * 失败或空结果时回退本地 SimpleRag + Chat Completions。
     * @param message 用户问题
     * @return 通用回答
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
        // 回退本地 RAG
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

    /**
     * 从 RAG 商品来源生成前端可渲染的「查看 / 加购」动作项
     * <p>
     * 该方法遍历 RAG 商品来源列表，提取商品 ID、名称、路径、URL、购物车信息等。
     * </p>
     * @param sources RAG 商品来源列表
     * @return 前端可渲染的「查看 / 加购」动作项列表
     */
    private List<Map<String, Object>> buildShoppingActions(List<Map<String, Object>> sources) {
        if (sources == null || sources.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> s : sources) {
            if (!"item".equals(String.valueOf(s.get("type")))) {
                continue;
            }
            Object idObj = s.get("id");
            if (idObj == null) {
                continue;
            }
            long itemId = idObj instanceof Number ? ((Number) idObj).longValue() : Long.parseLong(String.valueOf(idObj));
            Map<String, Object> action = new LinkedHashMap<>();
            action.put("type", "shopping_item");
            action.put("itemId", itemId);
            action.put("name", s.get("name"));
            action.put("productPath", s.get("productPath"));
            Object pu = s.get("productUrl");
            if (pu != null && !String.valueOf(pu).isBlank()) {
                action.put("productUrl", pu);
            }
            // 处理购物车信息
            Object rawCart = s.get("addToCart");
            if (rawCart instanceof Map<?, ?> && !((Map<?, ?>) rawCart).isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> cart = (Map<String, Object>) rawCart;
                action.put("addToCart", cart);
            }
            out.add(action);
        }
        return out;
    }

    /**
     * 通用导购回答
     * answer 回答内容
     * sources 回答来源
     */
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
     * @param message 用户输入的消息
     * @return 合成后的回答
     */
    private ChatResponse tryToolCall(String message) {
        if (message == null) {
            return null;
        }
        String m = message.trim();
        Long userId = UserContext.getUser();
        boolean loggedIn = userId != null;

        /* 工具调用动作 */
        List<Map<String, Object>> actions = new ArrayList<>();

        //TODO 这里是否优化一下，比如用正则表达式匹配优惠券、地址等关键词
        // 有可能会说我有那些优惠劵，我需要查询一下我的优惠券列表
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

        // TODO 可能需要优化一下，比如用正则表达式匹配地址等关键词
        // 有可能会说我有那些地址，我需要查询一下我的地址列表
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

        // TODO 可能需要优化一下，比如用正则表达式匹配我的信息等关键词
        // 有可能会说我有那些账号，我需要查询一下我的账号列表
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

        // TODO 可能需要优化一下，比如用正则表达式匹配订单等关键词
        // 有可能会说我有那些订单，我需要查询一下我的订单列表
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

    /**
     * 合成工具调用回答
     * @param userMessage 用户问题
     * @param sources 回答来源
     * @param fallback 回答失败时的默认回答
     * @return 合成回答
     */
    private String synthesizeToolAnswer(String userMessage, List<Map<String, Object>> sources, String fallback) {
        String key = llmProperties.getApiKey();
        if (key == null || key.isBlank()) {
            return fallback;
        }
        try {
            String json = objectMapper.writeValueAsString(sources);
            //TODO 问ai的prompt
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
