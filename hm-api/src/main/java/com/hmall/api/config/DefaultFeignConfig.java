package com.hmall.api.config;

import com.hmall.api.client.fallback.ItemClientFallback;
import com.hmall.common.utils.UserContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;

public class DefaultFeignConfig {
    @Bean
    public RequestInterceptor userInfoRequestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate requestTemplate) {
                Long userId = UserContext.getUser();
                if(userId != null){
                    requestTemplate.header("user-info", String.valueOf(userId));
                }
            }
        };
    }


    @Bean
    public ItemClientFallback itemClientFallback(){
        return new ItemClientFallback();
    }
}
