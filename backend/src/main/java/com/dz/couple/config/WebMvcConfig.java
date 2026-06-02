package com.dz.couple.config;

import com.dz.couple.security.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final AppProperties appProperties;

    @Autowired
    public WebMvcConfig(JwtInterceptor jwtInterceptor, AppProperties appProperties) {
        this.jwtInterceptor = jwtInterceptor;
        this.appProperties = appProperties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/health/**",
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/auth/password-reset/**"
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String dir = appProperties.getUploadDir();
        if (dir == null || dir.trim().isEmpty()) {
            dir = "uploads";
        }
        String abs = Paths.get(dir).toAbsolutePath().normalize().toString();
        String location = "file:" + (abs.endsWith("/") || abs.endsWith("\\") ? abs : abs + "/");
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }
}
