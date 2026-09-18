package com.neibarbosa.api_doc_generator.entity;

import jakarta.persistence.*;
import lombok.*;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="tarefas")
@Getter
@Setter
@AllArgsConstructor
@Builder
@NoArgsConstructor

public class Tarefa {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Identificador público da tarefa, exposto na API (ex: /api/tarefas/{codigo}).
     * Separado do "id" interno para não vazar informação sequencial do banco.
     */

    @Column(nullable = false, unique = true, updatable = false)
    private UUID codigo;

    @Column(nullable = false)
    private String urlRepositorio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusTarefa status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    private LocalDateTime dataInicio;

    private LocalDateTime dataFim;

    private String urlArtefato;

    @Column(length = 1000)
    private String mensagemErro;

    @PrePersist
    protected void aoCriar(){
        if (this.codigo == null){
            this.codigo = UUID.randomUUID();
        }
        if (this.dataCriacao == null){
            this.dataCriacao = LocalDateTime.now();
        }
        if (this.status == null){
            this.status = StatusTarefa.PENDENTE;
        }
    }
}
