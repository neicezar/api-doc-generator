package com.neibarbosa.api_doc_generator.service;

import com.neibarbosa.api_doc_generator.provedor.ProvedorRepositorio;
import com.neibarbosa.api_doc_generator.repository.TarefaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TarefaService{
    private final TarefaRepository tarefaRepository;
    private final List<ProvedorRepositorio> provedores;



}
