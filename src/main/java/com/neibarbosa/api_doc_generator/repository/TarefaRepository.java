package com.neibarbosa.api_doc_generator.repository;

import com.neibarbosa.api_doc_generator.entity.Tarefa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 Repositório de acesso a dados da entidade Tarefa.
 O Spring Data JPA gera a implementação automaticamente em tempo
 de execução — não é necessário escrever nenhuma classe concreta.
 */

public interface TarefaRepository extends JpaRepository<Tarefa, Long> {

    Optional<Tarefa> findByCodigo(UUID codigo);
}
