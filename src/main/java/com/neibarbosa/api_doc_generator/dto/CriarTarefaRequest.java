package com.neibarbosa.api_doc_generator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
/**
  Payload de entrada para disparar uma nova tarefa de geração de
  documentação. Usado como corpo da requisição POST /api/tarefas.

  Implementado como record: é um DTO imutável, sem necessidade de
  getters/setters manuais nem de Lombok — o próprio Java já gera
  o construtor, os acessores e equals/hashCode/toString.
 */
public record CriarTarefaRequest(

        @NotBlank(message = "A URL do repositório é obrigatória")
        @Pattern(
                regexp = "^https?://[\\w.-]+(:\\d+)?(/.*)?$",
                message = "A URL informada não é válida"
        )
        String urlRepositorio
) {
}
