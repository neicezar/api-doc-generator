package com.neibarbosa.api_doc_generator.config;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
/**
 * Configura o cliente HTTP do SDK oficial da OpenAI. A chave de API
 * vem de application.yml (openai.api-key), que por sua vez vem da
 * variável de ambiente OPENAI_API_KEY — nunca hardcoded.
 */
@Configuration
public class OpenAIConfig {

    @Bean
    public OpenAIClient openAIClient(@Value("${openai.api-key}") String apiKey) {
        return OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .timeout(Duration.ofSeconds(60))
                .build();
    }
}
