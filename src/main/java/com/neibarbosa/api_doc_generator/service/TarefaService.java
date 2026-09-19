package com.neibarbosa.api_doc_generator.service;

import com.neibarbosa.api_doc_generator.dto.CriarTarefaRequest;
import com.neibarbosa.api_doc_generator.dto.TarefaResponse;
import com.neibarbosa.api_doc_generator.entity.Tarefa;
import com.neibarbosa.api_doc_generator.exception.ProvedorNaoSuportadoException;
import com.neibarbosa.api_doc_generator.exception.TarefaNaoEncontradaException;
import com.neibarbosa.api_doc_generator.provedor.ProvedorRepositorio;
import com.neibarbosa.api_doc_generator.repository.TarefaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TarefaService{
    private final TarefaRepository tarefaRepository;
    private final List<ProvedorRepositorio> provedores;

    public TarefaResponse criarTarefa(CriarTarefaRequest request){
        ProvedorRepositorio provedor = localizarProvedor(request.urlRepositorio());

        Tarefa tarefa = Tarefa.builder()
                .urlRepositorio(request.urlRepositorio())
                .build();

        Tarefa tarefaSalva = tarefaRepository.save(tarefa);

        return TarefaResponse.fromEntity(tarefaSalva);
    }

    //Usando isPresent() e get() (Java 8+)
    public TarefaResponse buscarPorCodigo(UUID codigo){
        Optional<Tarefa> tarefaOptional = tarefaRepository.findByCodigo(codigo);
        if(!tarefaOptional.isPresent()){
            throw new TarefaNaoEncontradaException(codigo);
        }
        Tarefa tarefa = tarefaOptional.get();
        return TarefaResponse.fromEntity(tarefa);
    }
    //Usando expressão lambda
    private ProvedorRepositorio localizarProvedor(String urlRepositorio){
        return provedores.stream()
                .filter(p -> p.suporta(urlRepositorio))
                .findFirst()
                .orElseThrow(() -> new ProvedorNaoSuportadoException(urlRepositorio));
    }
}
