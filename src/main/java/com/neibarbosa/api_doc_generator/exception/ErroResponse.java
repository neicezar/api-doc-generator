package com.neibarbosa.api_doc_generator.exception;

import java.time.Instant;
import java.util.Map;

public record ErroResponse(
        Instant timestamp,
        int status,
        String erro,
        String mensagem,
        Map<String, String> camposInvalidos
) {
    public static ErroResponse of(int status, String erro, String mensagem){
        return new ErroResponse(Instant.now(), status, erro, mensagem, null);
    }

    public static ErroResponse deValidacao(Map<String, String> camposInvalidos) {
        return new ErroResponse(
                Instant.now(),
                400,
                "Bad Request",
                "Um ou mais campos inválidos",
                camposInvalidos
        );
    }
}
