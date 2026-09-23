package com.neibarbosa.api_doc_generator.controller;

import com.neibarbosa.api_doc_generator.dto.CriarTarefaRequest;
import com.neibarbosa.api_doc_generator.dto.TarefaResponse;
import com.neibarbosa.api_doc_generator.service.SseEmitterService;
import com.neibarbosa.api_doc_generator.service.TarefaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/tarefas")
@RequiredArgsConstructor
public class TarefaController {

    private final TarefaService tarefaService;
    private final SseEmitterService sseEmitterService;

    /**
     * Dispara uma nova tarefa de geração de documentação.
     *
     * Retorna 202 Accepted (não 200/201), porque o processamento é
     * assíncrono: neste momento a tarefa só foi criada e persistida
     * com status PENDENTE, o trabalho real ainda nem começou. O header
     * Location aponta para onde o cliente pode consultar o status.
     */
    @PostMapping
    public ResponseEntity<TarefaResponse> criarTarefa(
            @Valid @RequestBody CriarTarefaRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        TarefaResponse tarefaCriada = tarefaService.criarTarefa(request);

        URI location = uriBuilder
                .path("/api/tarefas/{codigo}")
                .buildAndExpand(tarefaCriada.codigo())
                .toUri();

        return ResponseEntity.accepted()
                .location(location)
                .body(tarefaCriada);
    }

    /**
     * Consulta o estado atual de uma tarefa pelo seu código público.
     */
    @GetMapping("/{codigo}")
    public ResponseEntity<TarefaResponse> buscarPorCodigo(@PathVariable UUID codigo) {
        return ResponseEntity.ok(tarefaService.buscarPorCodigo(codigo));
    }

    /**
     * Abre uma conexão SSE para acompanhar o progresso da tarefa em
     * tempo real. O cliente recebe eventos a cada mudança de status
     * (BAIXANDO → EXTRAINDO → GERANDO → CONCLUIDO ou FALHOU) sem
     * precisar fazer polling.
     *
     * Escolhemos SSE em vez de WebSocket porque a comunicação é
     * unidirecional — o servidor notifica o cliente, mas o cliente não
     * precisa enviar nada de volta durante o processamento.
     */
    @GetMapping(value = "/{codigo}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter acompanharTarefa(@PathVariable UUID codigo) {
        return sseEmitterService.registrar(codigo);
    }
}