package com.neibarbosa.api_doc_generator.extraction;

import java.util.List;

/**
 * Assinatura de um método público extraída via AST — sem o corpo,
 * já que só a assinatura interessa para gerar documentação.
 */
public record MetodoExtraido(
        String nome,
        List<String> parametros,
        String tipoRetorno,
        List<String> anotacoes
) {
}
