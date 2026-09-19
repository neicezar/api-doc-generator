package com.neibarbosa.api_doc_generator.exception;

import java.util.UUID;

public class TarefaNaoEncontradaException extends RuntimeException{

    public TarefaNaoEncontradaException(UUID codigo){
        super("Tarefa não encontrada: " + codigo);
    }
}
