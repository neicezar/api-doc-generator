package com.neibarbosa.api_doc_generator.extraction;

/**
 * Um campo (ou, no caso de um record, um componente) de uma classe
 * extraída — nome e tipo, sem valor, já que só a estrutura interessa
 * para documentação.
 */
public record CampoExtraido(String nome, String tipo) {
}

