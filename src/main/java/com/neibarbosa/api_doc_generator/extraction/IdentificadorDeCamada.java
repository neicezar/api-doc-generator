package com.neibarbosa.api_doc_generator.extraction;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Identifica a camada arquitetural de uma classe combinando múltiplos
 * sinais, do mais forte para o mais fraco — a mesma estratégia
 * definida no planejamento do projeto:
 */
@Component
public class IdentificadorDeCamada {


    private static final Set<String> ANOTACOES_CONTROLLER = Set.of("RestController", "Controller");
    private static final Set<String> ANOTACOES_SERVICE = Set.of("Service");
    private static final Set<String> ANOTACOES_ENTITY = Set.of("Entity");
    private static final Set<String> ANOTACOES_REPOSITORY = Set.of("Repository");
    private static final Set<String> ANOTACOES_VALIDACAO = Set.of(
            "NotBlank", "NotNull", "NotEmpty", "Size", "Pattern", "Email", "Min", "Max"
    );

    public CamadaClasse identificar(
            String nomePacote,
            String nomeClasse,
            List<String> anotacoesDeClasse,
            boolean possuiCampoComAnotacaoDeValidacao
    ) {
        // 1. Anotações — sinal mais forte
        if (contemAlguma(anotacoesDeClasse, ANOTACOES_CONTROLLER)) return CamadaClasse.CONTROLLER;
        if (contemAlguma(anotacoesDeClasse, ANOTACOES_SERVICE)) return CamadaClasse.SERVICE;
        if (contemAlguma(anotacoesDeClasse, ANOTACOES_ENTITY)) return CamadaClasse.ENTITY;
        if (contemAlguma(anotacoesDeClasse, ANOTACOES_REPOSITORY)) return CamadaClasse.REPOSITORY;

        // 2. Nome do pacote
        String pacote = nomePacote == null ? "" : nomePacote.toLowerCase();
        if (pacote.contains(".dto")) return CamadaClasse.DTO;
        if (pacote.contains(".controller")) return CamadaClasse.CONTROLLER;
        if (pacote.contains(".service")) return CamadaClasse.SERVICE;
        if (pacote.contains(".repository")) return CamadaClasse.REPOSITORY;
        if (pacote.contains(".entity")) return CamadaClasse.ENTITY;

        // 3. Sufixo do nome da classe
        if (nomeClasse.endsWith("Controller")) return CamadaClasse.CONTROLLER;
        if (nomeClasse.endsWith("Service") || nomeClasse.endsWith("ServiceImpl")) return CamadaClasse.SERVICE;
        if (nomeClasse.endsWith("Repository")) return CamadaClasse.REPOSITORY;
        if (nomeClasse.endsWith("DTO") || nomeClasse.endsWith("Request") || nomeClasse.endsWith("Response")) {
            return CamadaClasse.DTO;
        }

        // 4. Fallback: anotação de validação em campo, sem nenhum sinal acima
        if (possuiCampoComAnotacaoDeValidacao) return CamadaClasse.DTO;

        return CamadaClasse.DESCONHECIDA;
    }

    public boolean ehAnotacaoDeValidacao(String nomeAnotacao) {
        return ANOTACOES_VALIDACAO.contains(nomeAnotacao);
    }

    private boolean contemAlguma(List<String> anotacoesDaClasse, Set<String> candidatas) {
        return anotacoesDaClasse.stream().anyMatch(candidatas::contains);
    }
}