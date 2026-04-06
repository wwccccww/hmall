package com.hmall.ai.tool;

import com.hmall.ai.config.AiDownstreamProperties;
import com.hmall.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserTools {

    private final AiDownstreamProperties downstream;
    private final WebClient webClient = WebClient.builder().build();

    /** 禁止用 {@code Map.of} 组装业务数据：value 可能为 null，会触发 NPE */
    private static Map<String, Object> errorMap(String message) {
        Map<String, Object> m = new LinkedHashMap<>(2);
        m.put("error", message);
        return m;
    }

    private static LinkedHashMap<String, Object> shallowStringKeyMap(Object o) {
        if (!(o instanceof Map)) {
            return null;
        }
        Map<?, ?> raw = (Map<?, ?>) o;
        LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
        raw.forEach((k, v) -> copy.put(k != null ? String.valueOf(k) : "null", v));
        return copy;
    }

    public Map<String, Object> queryMe() {
        Long userId = UserContext.getUser();
        if (userId == null) {
            return errorMap("未登录");
        }
        Map<?, ?> resp = webClient.get()
                .uri(downstream.userBase() + "/users/me")
                .header("user-info", String.valueOf(userId))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        if (resp == null) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> out = new LinkedHashMap<>(4);
        Object remoteUserId = resp.get("userId");
        out.put("userId", remoteUserId != null ? remoteUserId : userId);
        out.put("role", resp.get("role"));
        return out;
    }

    public List<Map<String, Object>> queryMyAddresses() {
        Long userId = UserContext.getUser();
        if (userId == null) {
            return List.of(errorMap("未登录"));
        }
        List<?> list = webClient.get()
                .uri(downstream.userBase() + "/addresses")
                .header("user-info", String.valueOf(userId))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(List.class)
                .block();
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> out = new ArrayList<>(list.size());
        for (Object o : list) {
            if (!(o instanceof Map)) {
                continue;
            }
            Map<?, ?> m = (Map<?, ?>) o;
            Map<String, Object> row = new LinkedHashMap<>(8);
            row.put("id", m.get("id"));
            row.put("city", m.get("city"));
            row.put("town", m.get("town"));
            row.put("street", m.get("street"));
            row.put("province", m.get("province"));
            row.put("isDefault", m.get("isDefault"));
            row.put("contact", "***");
            row.put("mobile", "***");
            out.add(row);
        }
        return out;
    }

    public List<Map<String, Object>> queryMyCoupons() {
        Long userId = UserContext.getUser();
        if (userId == null) {
            return List.of(errorMap("未登录"));
        }
        List<?> list = webClient.get()
                .uri(downstream.promotionBase() + "/coupons/my")
                .header("user-info", String.valueOf(userId))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(List.class)
                .block();
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> out = new ArrayList<>(list.size());
        for (Object o : list) {
            LinkedHashMap<String, Object> row = shallowStringKeyMap(o);
            if (row != null) {
                out.add(row);
            }
        }
        return out;
    }

    public Map<String, Object> queryOrderById(Long orderId) {
        Long userId = UserContext.getUser();
        if (userId == null) {
            return errorMap("未登录");
        }
        if (orderId == null) {
            return errorMap("缺少订单号");
        }
        Map<String, Object> resp = webClient.get()
                .uri(downstream.tradeBase() + "/orders/" + orderId)
                .header("user-info", String.valueOf(userId))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        if (resp == null) {
            return errorMap("订单不存在或无权查看");
        }
        LinkedHashMap<String, Object> out = shallowStringKeyMap(resp);
        if (out == null) {
            return errorMap("订单不存在或无权查看");
        }
        // 与 queryMe 一致：若下游未带 userId，可用当前登录用户补足（需 trade 侧校验归属则更严谨）
        if (out.get("userId") == null) {
            out.put("userId", userId);
        }
        return out;
    }
}
