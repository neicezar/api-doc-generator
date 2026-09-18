package com.neibarbosa.api_doc_generator.exception;
/**
 Lançada quando nenhum ProvedorRepositorio disponível sabe processar
 a URL de repositório informada pelo usuário.

 Estende RuntimeException (unchecked) porque essa é uma falha de
 validação de entrada, não uma condição que o chamador deva ser
 forçado a tratar explicitamente com try/catch em todo lugar — ela
 será capturada de forma centralizada por um @ExceptionHandler
 global, que traduz para uma resposta HTTP apropriada (400 Bad Request).
 */
public class ProvedorNaoSuportadoException  extends RuntimeException {
    public ProvedorNaoSuportadoException(String urlRepositorio){
        super("Nenhum provedor suportado para a URL informada: " + urlRepositorio);
    }
}
