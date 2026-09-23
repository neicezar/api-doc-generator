package com.neibarbosa.api_doc_generator.extraction;

import java.util.List;
/**
 * Um parâmetro de método, com suas anotações — essencial para
 * documentar endpoints, já que é nas anotações do parâmetro que
 * ficam informações como o nome real do query param
 * (@RequestParam("name")), o valor padrão (defaultValue), se é
 * um path variable ou se é o corpo da requisição (@RequestBody).
 */
public record ParametroExtraido(
        String nome,
        String tipo,
        List<String> anotacoes
) {
}
