package com.neibarbosa.api_doc_generator.service;

import com.neibarbosa.api_doc_generator.entity.StatusTarefa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerencia as conexões SSE abertas — uma por tarefa que está sendo
 * acompanhada em tempo real pelo cliente.
 *
 * O ConcurrentHashMap é essencial aqui porque duas threads diferentes
 * acessam esse mapa simultaneamente: a thread do Tomcat (que registra
 * o emitter quando o cliente abre a conexão SSE) e a thread do
 * RabbitMQ listener (que notifica o emitter quando o status muda).
 */
@Service
public class SseEmitterService {
    private static final Logger log = LoggerFactory.getLogger(SseEmitterService.class);

    //Timeout de 10 minutos
    private static final long TIMEOUT_MS = 10 * 60 * 1000L;
    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();
    /**
     * Cria e registra um novo SseEmitter para a tarefa informada.
     * Chamado pelo TarefaController quando o cliente abre a conexão.
     */

    public SseEmitter registrar(UUID codigoTarefa){
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        emitters.put(codigoTarefa, emitter);
        // Garante que o emitter seja removido do mapa quando a conexão
        // encerrar — seja por conclusão, timeout ou erro do cliente.

        emitter.onCompletion(() -> emitters.remove(codigoTarefa));
        emitter.onTimeout(() -> emitters.remove(codigoTarefa));
        emitter.onError(ex -> emitters.remove(codigoTarefa));

        log.info("SSE registrado para tarefa {}", codigoTarefa);
        return emitter;
    }
    /**
     * Envia uma notificação de mudança de status para o cliente que
     * está acompanhando a tarefa. Chamado pelo TarefaProcessamentoService
     * a cada transição de status.
     *
     * Se não houver nenhum cliente conectado (emitter ausente), apenas
     * ignora — o cliente pode não ter aberto a conexão SSE, ou pode
     * ter desconectado. O status continua sendo salvo no banco de
     * qualquer forma, então o cliente pode consultar via GET quando
     * quiser.
     */
    public void notificar(UUID codigoTarefa, StatusTarefa status, String mensagem) {
        SseEmitter emitter = emitters.get(codigoTarefa);
        if (emitter == null) {
            return;
        }

        try {
            emitter.send(
                    SseEmitter.event()
                            .name(status.name())
                            .data(mensagem != null ? mensagem : status.name())
            );

            // Encerra a conexão quando a tarefa termina (sucesso ou falha)
            if (status == StatusTarefa.CONCLUIDO || status == StatusTarefa.FALHOU) {
                emitter.complete();
            }
        } catch (IOException ex) {
            log.warn("Falha ao enviar evento SSE para tarefa {} — cliente pode ter desconectado", codigoTarefa);
            emitters.remove(codigoTarefa);
        }
    }


}
