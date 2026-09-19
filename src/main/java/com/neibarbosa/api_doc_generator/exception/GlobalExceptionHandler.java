package com.neibarbosa.api_doc_generator.exception;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    //Nenhum ProvedorRepositorio soube processar a URL informada. Erro do cliente -> 400.
    @ExceptionHandler(ProvedorNaoSuportadoException.class)
    public ResponseEntity<ErroResponse> handleProvedorNaoSuportado(ProvedorNaoSuportadoException ex) {
        ErroResponse erro = ErroResponse.of(400, "Bad Request", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    //Nenhum tarefa encontrada para o código consultado -> 404.
    @ExceptionHandler(TarefaNaoEncontradaException.class)
    public ResponseEntity<ErroResponse> handleTarefaNaoEncontrada(TarefaNaoEncontradaException ex) {
        ErroResponse erro = ErroResponse.of(404, "Not Found", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(erro);
    }

    //Falha de validação do @Valid/@Pattern/@NotBlank nos DTOs de entrada
    @ExceptionHandler(MethodArgumentNotValidException .class)
    public ResponseEntity<ErroResponse> handleValidacao(MethodArgumentNotValidException  ex){
        Map<String, String> camposInvalidos = new LinkedHashMap<>();
        for (FieldError  fieldError : ex.getBindingResult().getFieldErrors()){
            camposInvalidos.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErroResponse.deValidacao(camposInvalidos));

    }

    // Registra o erro real no log do servidor (para investigação)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> handle(Exception ex){
        log.error("Erro não tratado", ex);
        ErroResponse erro = ErroResponse.of(500, "Internal Server Error", "Ocorreu um erro inesperado.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(erro);
    }

}
