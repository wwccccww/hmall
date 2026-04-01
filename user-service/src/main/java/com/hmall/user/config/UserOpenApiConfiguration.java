package com.hmall.user.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / springdoc：补充文档说明与安全方案，便于在 doc.html 中调试。
 */
@Configuration
public class UserOpenApiConfiguration {

    /** 与 {@link com.hmall.common.interceptor.UserInfoInterceptor} 一致 */
    public static final String USER_INFO_SCHEME = "user-info";
    public static final String BEARER_SCHEME = "bearer-jwt";

    @Bean
    public OpenAPI userServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("用户服务 API")
                        .version("v1.0.0")
                        .description(
                                "本服务端口默认 8084，文档入口：/doc.html 。\n\n"
                                        + "**直连调试**：除登录外，需带请求头 `user-info`，值为用户数字 ID。"
                                        + "可先调用「用户登录」接口，从响应中的 `userId` 复制到 Authorize 里的 user-info。\n\n"
                                        + "**Bearer**：与网关约定一致；user-service 本进程不解析 JWT，"
                                        + "仅作文档说明，便于与经网关调用时的习惯对齐。"))
                .components(new Components()
                        .addSecuritySchemes(USER_INFO_SCHEME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("user-info")
                                        .description("当前用户 ID（Long），与网关向下游注入的 user-info 一致"))
                        .addSecuritySchemes(BEARER_SCHEME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("登录接口返回的 token（经网关访问时使用）")));
    }
}
