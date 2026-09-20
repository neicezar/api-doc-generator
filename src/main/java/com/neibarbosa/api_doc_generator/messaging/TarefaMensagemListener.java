package com.neibarbosa.api_doc_generator.messaging;

import com.neibarbosa.api_doc_generator.config.RabbitMQConfig;
import com.neibarbosa.api_doc_generator.service.TarefaProcessamentoService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consome as mensagens da fila de tarefas e delega o processamento
 * real para o TarefaProcessamentoService. Mantido intencionalmente
 * fino — a lógica de negócio (baixar, extrair, gerar documentação)
 * mora no service, não aqui, seguindo o mesmo padrão já aplicado ao
 * TarefaController.
 */
@Component
@RequiredArgsConstructor
public class TarefaMensagemListener {

    private static final Logger log = LoggerFactory.getLogger(TarefaMensagemListener.class);

    private final TarefaProcessamentoService tarefaProcessamentoService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void receberTarefa(String codigo) {
        log.info("Tarefa recebida da fila para processamento: {}", codigo);
        tarefaProcessamentoService.processar(UUID.fromString(codigo));
    }
}