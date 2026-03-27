package com.hmall.gateway.filters;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.AntPathMatcher;
import com.hmall.gateway.config.AuthProperties;
import com.hmall.gateway.utils.JwtTool;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
@EnableConfigurationProperties({AuthProperties.class})
public class AuthGlobalFilter implements GlobalFilter , Ordered {

    private final JwtTool jwtTool;

    private final AuthProperties authProperties;

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

//   Mono<Void> filter “异步执行完拦截逻辑，不返回具体数据”。
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        //判断是否需要拦截
        if(isExclude(request.getPath().toString())){
            return chain.filter(exchange);
        }
        //获取token
        String authorization = "authorization";
        List<String> headers = request.getHeaders().get(authorization);
        String token = null;
        if(!CollUtil.isEmpty(headers)){
            token = headers.get(0);
        }
        //解析token
        Long userId = null;
        try {
            userId = jwtTool.parseToken(token);
        } catch (Exception e) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
//            response.setStatusCode(HttpStatus.valueOf(401));
            return response.setComplete();
        }

        System.out.println("userId:"+userId);

        String userInfo = userId.toString();
        //这个传不到下游微服务
//        exchange.getAttributes().put("user-info", userInfo);
        ServerWebExchange ex = exchange.mutate()
                .request(b -> b.header("user-info", userInfo))
                .build();

        return chain.filter(ex);
    }

    private boolean isExclude(String authPath) {
        for (String pathPattern : authProperties.getExcludePaths() ) {
            if (antPathMatcher.match(pathPattern, authPath)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
