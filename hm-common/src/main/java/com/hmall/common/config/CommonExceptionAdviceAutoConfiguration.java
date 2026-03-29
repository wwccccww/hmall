package com.hmall.common.config;

import com.hmall.common.advice.CommonExceptionAdvice;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 将 {@link CommonExceptionAdvice} 纳入自动装配：各微服务主类仅扫描自身包时也能注册全局异常处理。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import(CommonExceptionAdvice.class)
public class CommonExceptionAdviceAutoConfiguration {
}
