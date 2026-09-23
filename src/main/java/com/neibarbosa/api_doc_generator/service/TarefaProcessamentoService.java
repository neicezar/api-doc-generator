package com.neibarbosa.api_doc_generator.service;

import com.neibarbosa.api_doc_generator.entity.StatusTarefa;
import com.neibarbosa.api_doc_generator.entity.Tarefa;
import com.neibarbosa.api_doc_generator.exception.TarefaNaoEncontradaException;
import com.neibarbosa.api_doc_generator.extraction.CamadaClasse;
import com.neibarbosa.api_doc_generator.extraction.ClasseExtraida;
import com.neibarbosa.api_doc_generator.extraction.ExtratorDeCodigo;
import com.neibarbosa.api_doc_generator.provedor.ProvedorLocator;
import com.neibarbosa.api_doc_generator.provedor.ProvedorRepositorio;
import com.neibarbosa.api_doc_generator.repository.TarefaRepository;
import com.neibarbosa.api_doc_generator.service.llm.GeradorDeDocumentacaoService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Orquestra o pipeline de processamento de uma tarefa, acionado pelo
 * TarefaMensagemListener assim que uma mensagem chega na fila:
 *
 * baixar o repositório (ProvedorRepositorio) → extrair a estrutura
 * do código (ExtratorDeCodigo) → gerar a documentação via LLM
 * (GeradorDeDocumentacaoService, map-reduce) → salvar o resultado.
 *
 * Cada etapa atualiza o status da Tarefa no banco, para que o
 * frontend consiga acompanhar o progresso.
 */
@Service
@RequiredArgsConstructor
public class TarefaProcessamentoService {

    private static final Logger log = LoggerFactory.getLogger(TarefaProcessamentoService.class);

    private final TarefaRepository tarefaRepository;
    private final ProvedorLocator provedorLocator;
    private final ExtratorDeCodigo extratorDeCodigo;
    private final GeradorDeDocumentacaoService geradorDeDocumentacaoService;

    public void processar(UUID codigo) {
        Tarefa tarefa = tarefaRepository.findByCodigo(codigo)
                .orElseThrow(() -> new TarefaNaoEncontradaException(codigo));

        try {
            baixarEExtrair(tarefa);
        } catch (Exception ex) {
            log.error("Falha ao processar tarefa {}", codigo, ex);
            tarefa.setStatus(StatusTarefa.FALHOU);
            tarefa.setMensagemErro(mensagemDeErroSegura(ex));
            tarefa.setDataFim(LocalDateTime.now());
            tarefaRepository.save(tarefa);
        }
    }

    private void baixarEExtrair(Tarefa tarefa) throws Exception {
        tarefa.setStatus(StatusTarefa.BAIXANDO);
        tarefa.setDataInicio(LocalDateTime.now());
        tarefaRepository.save(tarefa);

        ProvedorRepositorio provedor = provedorLocator.localizar(tarefa.getUrlRepositorio());

        try (InputStream zipStream = provedor.baixarRepositorio(tarefa.getUrlRepositorio(), null)) {
            tarefa.setStatus(StatusTarefa.EXTRAINDO);
            tarefaRepository.save(tarefa);

            List<ClasseExtraida> classes = extratorDeCodigo.extrair(zipStream);

            Map<CamadaClasse, Long> contagemPorCamada = classes.stream()
                    .collect(Collectors.groupingBy(ClasseExtraida::camada, Collectors.counting()));

            log.info(
                    "Extração concluída para tarefa {}: {} classes encontradas — {}",
                    tarefa.getCodigo(), classes.size(), contagemPorCamada
            );

            String documentacao = geradorDeDocumentacaoService.gerarDocumentacao(
                    tarefa.getUrlRepositorio(), classes
            );

            String caminhoArquivo = salvarDocumentacao(tarefa.getCodigo(), documentacao);

            tarefa.setStatus(StatusTarefa.CONCLUIDO);
            tarefa.setUrlArtefato(caminhoArquivo);
            tarefa.setDataFim(LocalDateTime.now());
        }

        tarefaRepository.save(tarefa);
    }

    /**
     * Salva a documentação gerada em disco local. Um destino provisório
     * — numa evolução futura do projeto, isso viraria um upload para
     * armazenamento em nuvem (ex: AWS S3), com urlArtefato passando a
     * ser uma URL pública em vez de um caminho de arquivo local.
     */
    private String salvarDocumentacao(UUID codigo, String conteudoMarkdown) throws Exception {
        Path diretorio = Path.of("documentos-gerados");
        Files.createDirectories(diretorio);

        Path arquivo = diretorio.resolve(codigo + ".md");
        Files.writeString(arquivo, conteudoMarkdown);

        return arquivo.toAbsolutePath().toString();
    }

    /**
     * Nunca expõe a mensagem bruta de exceptions inesperadas (poderia
     * vazar detalhe interno do sistema) — só as que já vêm com uma
     * mensagem pensada para ser lida por um humano, como as exceptions
     * de domínio da aplicação.
     */
    private String mensagemDeErroSegura(Exception ex) {
        if (ex.getMessage() != null && ex.getMessage().length() < 500) {
            return ex.getMessage();
        }
        return "Falha inesperada durante o processamento da tarefa.";
    }
}