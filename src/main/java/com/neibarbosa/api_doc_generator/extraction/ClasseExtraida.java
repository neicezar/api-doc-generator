package com.neibarbosa.api_doc_generator.extraction;

import java.util.List;

/**
 * Representação estruturada de uma classe/interface Java extraída do
 * repositório — o JSON compacto que será enviado à LLM no lugar do
 * código-fonte bruto (Fase 5), evitando estourar o contexto do
 * modelo com arquivos inteiros.
 */
public record ClasseExtraida(
        String nomePacote,
        String nomeClasse,
        CamadaClasse camada,
        List<String> anotacoesDeClasse,
        List<MetodoExtraido> metodos
) {
}
