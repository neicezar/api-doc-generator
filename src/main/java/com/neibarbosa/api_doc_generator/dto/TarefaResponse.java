package com.neibarbosa.api_doc_generator.dto;

import com.neibarbosa.api_doc_generator.entity.StatusTarefa;
import com.neibarbosa.api_doc_generator.entity.Tarefa;

import java.util.UUID;

public record TarefaResponse(
        UUID codigo,
        StatusTarefa status,
        String urlRepositorio,
        String urlArtefato,
        String mensagemErro
) {
    public static TarefaResponse fromEntity(Tarefa tarefa){
        return new TarefaResponse(
                tarefa.getCodigo(),
                tarefa.getStatus(),
                tarefa.getUrlRepositorio(),
                tarefa.getUrlArtefato(),
                tarefa.getMensagemErro()
        );
    }
}
