package com.neibarbosa.api_doc_generator.messaging;

import com.neibarbosa.api_doc_generator.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TarefaMensagemPublisher {
    private final RabbitTemplate rabbitTemplate;

    public void publicarNovaTarefa(UUID codigo){
        rabbitTemplate.convertAndSend(
                        RabbitMQConfig.EXCHANGE,
                        RabbitMQConfig.ROUTING_KEY,
                        codigo.toString()
                );

    }
}
