package com.neibarbosa.api_doc_generator.service.llm;

import com.neibarbosa.api_doc_generator.extraction.ClasseExtraida;

import java.util.List;

/**
 * Representa um pacote Java com suas classes extraídas e, após a
 * etapa 2 do pipeline map-reduce, a documentação gerada pela LLM
 * para aquele pacote especificamente.
 *
 * Usado como unidade de processamento entre as etapas do pipeline:
 * - após a etapa 1 (map), cada instância tem classes e resumos
 * - após a etapa 2, também tem documentação consolidada do pacote
 */
public record PacoteDocumentado(
        String nomePacote,
        List<ClasseExtraida> classes,
        List<String> resumosDasClasses,
        String documentacao
) {

    /**
     * Cria um PacoteDocumentado sem documentação ainda (após a etapa 1).
     */
    public static PacoteDocumentado semDocumentacao(
            String nomePacote,
            List<ClasseExtraida> classes,
            List<String> resumos
    ) {
        return new PacoteDocumentado(nomePacote, classes, resumos, null);
    }

    /**
     * Retorna uma cópia com a documentação preenchida (após a etapa 2).
     */
    public PacoteDocumentado comDocumentacao(String documentacao) {
        return new PacoteDocumentado(nomePacote, classes, resumosDasClasses, documentacao);
    }

    /**
     * Resumo compacto do pacote para uso como contexto nas etapas 2 e 3
     * — mostra o papel do pacote sem incluir o detalhe completo das classes.
     */
    public String resumoCompacto() {
        String classes = this.classes.stream()
                .map(c -> "%s (%s)".formatted(c.nomeClasse(), c.camada()))
                .collect(java.util.stream.Collectors.joining(", "));
        return "Pacote '%s': %s".formatted(nomePacote, classes);
    }
}
