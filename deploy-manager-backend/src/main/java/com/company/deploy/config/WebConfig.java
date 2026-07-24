package com.company.deploy.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置。
 * CORS 配置已迁移至 SecurityConfig，避免与 Spring Security 冲突。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
}
