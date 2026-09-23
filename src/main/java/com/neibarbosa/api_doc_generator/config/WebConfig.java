package com.neibarbosa.api_doc_generator.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
/**
 * Libera o CORS para o frontend Angular (localhost:4200) acessar a
 * API (localhost:8080). Em produção, substituir pela URL real do
 * frontend.
 *
 * Sem essa configuração, o navegador bloquearia todas as chamadas
 * cross-origin do Angular para o Spring Boot por causa da política
 * de segurança Same-Origin.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:4200")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false);
    }
}
