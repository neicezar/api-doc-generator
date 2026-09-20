package com.neibarbosa.api_doc_generator.provedor;

import com.neibarbosa.api_doc_generator.exception.ProvedorNaoSuportadoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Centraliza a escolha de qual ProvedorRepositorio deve tratar uma
 * URL. Extraído do TarefaService porque mais de uma classe precisa
 * dessa mesma decisão (TarefaService, ao criar a tarefa, e
 * TarefaProcessamentoService, ao efetivamente baixar o repositório) —
 * duplicar essa lógica em dois lugares seria um convite a divergência
 * futura caso a regra de escolha mude.
 */
@Component
@RequiredArgsConstructor
public class ProvedorLocator {
    private final List<ProvedorRepositorio> provedores;

    public ProvedorRepositorio localizador (String urlRepositorio){
        return provedores.stream()
                .filter(p -> p.suporta(urlRepositorio))
                .findFirst()
                .orElseThrow(() -> new ProvedorNaoSuportadoException(urlRepositorio));
    }
}
