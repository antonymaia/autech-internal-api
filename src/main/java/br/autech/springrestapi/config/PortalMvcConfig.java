package br.autech.springrestapi.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class PortalMvcConfig implements WebMvcConfigurer {

    private final PortalRateLimitInterceptor portalRateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(portalRateLimitInterceptor)
                .addPathPatterns("/pub/cliente/**");
    }
}
