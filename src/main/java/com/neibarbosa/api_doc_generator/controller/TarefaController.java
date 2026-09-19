package com.neibarbosa.api_doc_generator.controller;

import com.neibarbosa.api_doc_generator.dto.CriarTarefaRequest;
import com.neibarbosa.api_doc_generator.dto.TarefaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import com.neibarbosa.api_doc_generator.service.TarefaService;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/tarefas")
@RequiredArgsConstructor
public class TarefaController {

    private final TarefaService tarefaService;

    @PostMapping
    public ResponseEntity<TarefaResponse> criarTarefa(
            @Valid @RequestBody CriarTarefaRequest request,
            UriComponentsBuilder uriBuilder
            ) {
        TarefaResponse tarefaCriada = tarefaService.criarTarefa(request);

        URI location = uriBuilder
                .path("/api/tarefas/{codigo}")
                .buildAndExpand(tarefaCriada.codigo())
                .toUri();

        return ResponseEntity.accepted()
                .location(location)
                .body(tarefaCriada);
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<TarefaResponse> buscarPorCodigo(@PathVariable UUID codigo) {
        return ResponseEntity.ok(tarefaService.buscarPorCodigo(codigo));
    }
}
