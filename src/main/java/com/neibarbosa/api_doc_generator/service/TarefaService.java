package com.neibarbosa.api_doc_generator.service;

import com.neibarbosa.api_doc_generator.dto.CriarTarefaRequest;
import com.neibarbosa.api_doc_generator.dto.TarefaResponse;
import com.neibarbosa.api_doc_generator.entity.Tarefa;
import com.neibarbosa.api_doc_generator.exception.TarefaNaoEncontradaException;
import com.neibarbosa.api_doc_generator.messaging.TarefaMensagemPublisher;
import com.neibarbosa.api_doc_generator.provedor.ProvedorLocator;
import com.neibarbosa.api_doc_generator.repository.TarefaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Orquestra a criação de uma nova tarefa: valida qual provedor sabe
 * lidar com a URL informada, persiste a tarefa com status inicial, e
 * publica a mensagem no RabbitMQ que dispara o processamento
 * assíncrono real (feito pelo TarefaProcessamentoService, acionado
 * pelo TarefaMensagemListener).
 */
@Service
@RequiredArgsConstructor
public class TarefaService {

    private final TarefaRepository tarefaRepository;
    private final ProvedorLocator provedorLocator;
    private final TarefaMensagemPublisher tarefaMensagemPublisher;

    public TarefaResponse criarTarefa(CriarTarefaRequest request) {
        // Valida logo na criação que existe um provedor capaz de tratar
        // essa URL, mesmo sem baixar nada ainda — falha rápido, sem
        // enfileirar uma tarefa que o processamento não conseguiria tratar.
        provedorLocator.localizar(request.urlRepositorio());

        Tarefa tarefa = Tarefa.builder()
                .urlRepositorio(request.urlRepositorio())
                .build();

        Tarefa tarefaSalva = tarefaRepository.save(tarefa);

        tarefaMensagemPublisher.publicarNovaTarefa(tarefaSalva.getCodigo());

        return TarefaResponse.fromEntity(tarefaSalva);
    }

    public TarefaResponse buscarPorCodigo(UUID codigo) {
        Tarefa tarefa = tarefaRepository.findByCodigo(codigo)
                .orElseThrow(() -> new TarefaNaoEncontradaException(codigo));

        return TarefaResponse.fromEntity(tarefa);
    }
}