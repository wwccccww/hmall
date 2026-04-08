package com.hmall.common.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.PriorityOrdered;
import org.springframework.lang.NonNull;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import springfox.documentation.spring.web.plugins.WebMvcRequestHandlerProvider;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Springfox 2.x 在 Spring Boot 2.6+ 下会碰上 PathPattern / 无 {@link org.springframework.web.servlet.mvc.condition.PatternsRequestCondition} 的映射，
 * {@code WebMvcPatternsRequestConditionWrapper#getPatterns} 会 NPE。此处收缩 {@link WebMvcRequestHandlerProvider} 内的 handlerMappings 列表。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(WebMvcRequestHandlerProvider.class)
public class SpringfoxWebMvcHandlerWorkaroundAutoConfiguration {

    @Bean
    public static BeanPostProcessor springfoxWebMvcHandlerProviderWorkaround() {
        return new SpringfoxHandlerMappingFilter();
    }

    /** 具名类以便实现 {@link PriorityOrdered}，Spring 才会对其排序。 */
    private static final class SpringfoxHandlerMappingFilter implements BeanPostProcessor, PriorityOrdered {

        @Override
        public int getOrder() {
            return PriorityOrdered.LOWEST_PRECEDENCE;
        }

        @Override
        public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
            if (bean instanceof WebMvcRequestHandlerProvider) {
                filterMappings(bean);
            }
            return bean;
        }

        @SuppressWarnings("unchecked")
        private void filterMappings(Object bean) {
            Field field = ReflectionUtils.findField(WebMvcRequestHandlerProvider.class, "handlerMappings");
            if (field == null) {
                return;
            }
            ReflectionUtils.makeAccessible(field);
            try {
                List<RequestMappingInfoHandlerMapping> mappings =
                    (List<RequestMappingInfoHandlerMapping>) field.get(bean);
                if (mappings == null || mappings.isEmpty()) {
                    return;
                }
                List<RequestMappingInfoHandlerMapping> filtered = mappings.stream()
                    .filter(SpringfoxWebMvcHandlerWorkaroundAutoConfiguration::isSpringfoxCompatible)
                    .collect(Collectors.toList());
                try {
                    mappings.clear();
                    mappings.addAll(filtered);
                } catch (UnsupportedOperationException ignored) {
                    field.set(bean, new ArrayList<>(filtered));
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /**
     * 仅保留 Springfox 能安全包装的路径：Ant 解析、每条 RequestMappingInfo 有 PatternsRequestCondition 且无 PathPatternsCondition。
     */
    private static boolean isSpringfoxCompatible(RequestMappingInfoHandlerMapping mapping) {
        if (!(mapping instanceof RequestMappingHandlerMapping)) {
            return false;
        }
        RequestMappingHandlerMapping rmm = (RequestMappingHandlerMapping) mapping;
        if (rmm.getPatternParser() != null) {
            return false;
        }
        if (mapping.getHandlerMethods().isEmpty()) {
            return true;
        }
        for (RequestMappingInfo info : mapping.getHandlerMethods().keySet()) {
            if (info.getPathPatternsCondition() != null) {
                return false;
            }
            if (info.getPatternsCondition() == null) {
                return false;
            }
        }
        return true;
    }
}
