package com.demo.auth.config;

import com.demo.auth.interceptor.CustomInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置類 - 註冊攔截器
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final CustomInterceptor customInterceptor;

    public WebConfig(CustomInterceptor customInterceptor) {
        this.customInterceptor = customInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(customInterceptor)
                .addPathPatterns("/**")  // 攔截所有路徑
                .excludePathPatterns(    // 排除的路徑
                    "/api/auth/login",
                    "/api/auth/register",
                    "/h2-console/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/webjars/**",
                    "/*.js",
                    "/*.css",
                    "/*.png",
                    "/*.jpg",
                    "/*.jpeg",
                    "/*.gif",
                    "/*.ico"
                );
    }
}
