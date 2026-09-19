package com.neibarbosa.api_doc_generator.messaging;

import com.neibarbosa.api_doc_generator.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class TarefaMensagemListener {

    private static final Logger log = LoggerFactory.getLogger(TarefaMensagemListener.class);
    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void receberTarefa(String codigo) {
        log.info("Tarefa recebida da fila para processamento: {}", codigo);
    }
}
