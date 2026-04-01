package com.hmall.ai.tool;

import com.hmall.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserTools {

    private final WebClient webClient = WebClient.builder().build();

    public Map<String, Object> queryMe() {
        Long userId = UserContext.getUser();
        if (userId == null) {
            return Map.of("error", "未登录");
        }
        // user-service 的 /users/me 返回 UserLoginVO（token,userId,role...），这里只取必要字段
        Map<?, ?> resp = webClient.get()
                .uri("http://user-service:8084/users/me")
                .header("user-info", String.valueOf(userId))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        if (resp == null) return Map.of();
        return Map.of(
                "userId", resp.get("userId"),
                "role", resp.get("role")
        );
    }

    public List<Map<String, Object>> queryMyAddresses() {
        Long userId = UserContext.getUser();
        if (userId == null) {
            return List.of(Map.of("error", "未登录"));
        }
        List<Map<String, Object>> list = webClient.get()
                .uri("http://user-service:8084/addresses")
                .header("user-info", String.valueOf(userId))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(List.class)
                .block();
        if (list == null) return List.of();
        // 脱敏：只返回必要字段
        return list.stream().map(m -> Map.of(
                "id", m.get("id"),
                "city", m.get("city"),
                "town", m.get("town"),
                "street", m.get("street"),
                "contact", "***",
                "mobile", "***"
        )).toList();
    }

    public List<Map<String, Object>> queryMyCoupons() {
        Long userId = UserContext.getUser();
        if (userId == null) {
            return List.of(Map.of("error", "未登录"));
        }
        List<Map<String, Object>> list = webClient.get()
                .uri("http://promotion-service:8087/coupons/my")
                .header("user-info", String.valueOf(userId))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(List.class)
                .block();
        return list == null ? List.of() : list;
    }

    public Map<String, Object> queryOrderById(Long orderId) {
        Long userId = UserContext.getUser();
        if (userId == null) {
            return Map.of("error", "未登录");
        }
        if (orderId == null) {
            return Map.of("error", "缺少订单号");
        }
        Map<String, Object> resp = webClient.get()
                .uri("http://trade-service:8085/orders/" + orderId)
                .header("user-info", String.valueOf(userId))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        return resp == null ? Map.of() : resp;
    }
}

