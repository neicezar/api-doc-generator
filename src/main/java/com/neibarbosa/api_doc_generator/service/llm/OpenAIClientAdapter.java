package com.neibarbosa.api_doc_generator.service.llm;

import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OpenAIClientAdapter implements LLMClient {

    private final OpenAIClient client;
    private final String model;

    public OpenAIClientAdapter(OpenAIClient client, @Value("${openai.model}") String model) {
        this.client = client;
        this.model = model;
    }


    @Override
    public String gerarTexto(String promopt) {
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(model)
                .addUserMessage(promopt)
                .build();
        ChatCompletion completion = client.chat().completions().create(params);
        return completion.choices().get(0).message().content().orElse("");
    }
}
