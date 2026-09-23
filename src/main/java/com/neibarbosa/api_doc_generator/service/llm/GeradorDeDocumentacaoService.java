package com.neibarbosa.api_doc_generator.service.llm;

import com.neibarbosa.api_doc_generator.extraction.CampoExtraido;
import com.neibarbosa.api_doc_generator.extraction.ClasseExtraida;
import com.neibarbosa.api_doc_generator.extraction.MetodoExtraido;
import com.neibarbosa.api_doc_generator.extraction.ParametroExtraido;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Orquestra a geração da documentação final usando um pipeline de
 * 4 etapas inspirado no padrão map-reduce:
 *
 * ETAPA 0 — Detecta a arquitetura do projeto (camadas, hexagonal, etc.)
 *            a partir dos nomes de pacote e classes, gerando um contexto
 *            que guia as etapas seguintes.
 *
 * ETAPA 1 — MAP: resume cada classe individualmente (uma chamada por
 *            classe), preservando os detalhes técnicos que as etapas
 *            seguintes precisarão.
 *
 * ETAPA 2 — REDUCE por pacote: documenta cada pacote com profundidade,
 *            recebendo o contexto da arquitetura (etapa 0) + um resumo
 *            compacto de todos os pacotes (para entender o papel de
 *            cada grupo no sistema) + os resumos detalhados das classes
 *            do pacote que está sendo documentado naquele momento.
 *
 * ETAPA 3 — CONSOLIDAÇÃO final: monta o documento completo recebendo
 *            o contexto da arquitetura + as documentações de cada pacote.
 */
@Service
@RequiredArgsConstructor
public class GeradorDeDocumentacaoService {

    private final LLMClient llmClient;

    public String gerarDocumentacao(String urlRepositorio, List<ClasseExtraida> classes) {
        // Agrupa por pacote antes de qualquer chamada à LLM
        Map<String, List<ClasseExtraida>> porPacote = classes.stream()
                .collect(Collectors.groupingBy(
                        c -> c.nomePacote().isBlank() ? "(raiz)" : c.nomePacote(),
                        Collectors.toList()
                ));

        // Etapa 0: detecta a arquitetura do projeto
        String contextoArquitetura = detectarArquitetura(porPacote);

        // Etapa 1: resume cada classe (map individual)
        List<PacoteDocumentado> pacotes = porPacote.entrySet().stream()
                .map(entry -> {
                    List<String> resumos = entry.getValue().stream()
                            .map(this::resumirClasse)
                            .collect(Collectors.toList());
                    return PacoteDocumentado.semDocumentacao(entry.getKey(), entry.getValue(), resumos);
                })
                .collect(Collectors.toList());

        // Monta o resumo compacto de todos os pacotes (contexto global)
        String resumoGlobalDePacotes = pacotes.stream()
                .map(PacoteDocumentado::resumoCompacto)
                .collect(Collectors.joining("\n"));

        // Etapa 2: documenta cada pacote com o contexto completo
        List<PacoteDocumentado> pacotesDocumentados = pacotes.stream()
                .map(p -> p.comDocumentacao(
                        documentarPacote(p, contextoArquitetura, resumoGlobalDePacotes)
                ))
                .collect(Collectors.toList());

        // Etapa 3: consolida tudo no documento final
        return consolidar(urlRepositorio, contextoArquitetura, pacotesDocumentados);
    }

    // ── Etapa 0 ──────────────────────────────────────────────────────────────

    private String detectarArquitetura(Map<String, List<ClasseExtraida>> porPacote) {
        String listaDePacotes = porPacote.entrySet().stream()
                .map(entry -> "Pacote '%s': %s".formatted(
                        entry.getKey(),
                        entry.getValue().stream()
                                .map(c -> "%s (%s)".formatted(c.nomeClasse(), c.camada()))
                                .collect(Collectors.joining(", "))
                ))
                .collect(Collectors.joining("\n"));

        String prompt = """
                Com base na lista de pacotes e classes abaixo, identifique
                qual padrão arquitetural esse projeto Java segue (ex: camadas
                tradicionais controller/service/repository, hexagonal/ports-and-adapters,
                MVC, CQRS, ou outro) e explique brevemente o papel de cada pacote
                dentro dessa arquitetura.

                Responda em no máximo um parágrafo mais uma lista de bullets
                (um por pacote), de forma técnica e objetiva.
                Não inclua saudação nem comentário fora dessa descrição.

                Pacotes e classes do projeto:
                %s
                """.formatted(listaDePacotes);

        return llmClient.gerarTexto(prompt);
    }

    // ── Etapa 1 ──────────────────────────────────────────────────────────────

    private String resumirClasse(ClasseExtraida classe) {
        String prompt = """
                Resuma de forma técnica e objetiva o que a classe Java abaixo
                faz, para uso em documentação de API. Máximo de 4 frases.
                Se a classe tiver campos listados abaixo, você já tem a
                estrutura real dela — não diga que a estrutura "não foi
                detalhada" ou algo do tipo, apenas descreva o que já foi
                fornecido.

                Importante: este resumo será usado depois para montar o guia
                de endpoints e exemplos de requisição/resposta da API. Por
                isso, PRESERVE os detalhes técnicos exatos que aparecem
                abaixo — nomes e tipos dos campos, path das anotações de
                mapping (ex: o "/greeting" dentro de GetMapping), nomes dos
                parâmetros e seus valores padrão. Não os generalize nem os
                omita no resumo.

                Regras obrigatórias:
                - Baseie-se EXCLUSIVAMENTE nas informações fornecidas abaixo.
                - Responda APENAS com o resumo em si, sem saudação nem
                  comentário final.

                Pacote: %s
                Classe: %s
                Camada: %s
                Anotações: %s
                Campos: %s
                Métodos públicos: %s
                """.formatted(
                classe.nomePacote(),
                classe.nomeClasse(),
                classe.camada(),
                classe.anotacoesDeClasse(),
                formatarCampos(classe.campos()),
                formatarMetodos(classe.metodos())
        );

        return "### %s (%s)\n%s".formatted(
                classe.nomeClasse(), classe.camada(), llmClient.gerarTexto(prompt)
        );
    }

    // ── Etapa 2 ──────────────────────────────────────────────────────────────

    private String documentarPacote(
            PacoteDocumentado pacote,
            String contextoArquitetura,
            String resumoGlobalDePacotes
    ) {
        String prompt = """
                Você está documentando o pacote '%s' de uma API Java.

                CONTEXTO ARQUITETURAL DO PROJETO:
                %s

                VISÃO GERAL DE TODOS OS PACOTES (para entender o papel de
                cada grupo no sistema — não detalhe esses outros pacotes,
                só use como contexto):
                %s

                CLASSES DESTE PACOTE (detalhe completo — documente todas):
                %s

                Com base nisso, gere a documentação deste pacote em Markdown,
                incluindo:
                - Uma descrição do papel deste pacote na arquitetura
                - Para controladores: os endpoints que expõe, com parâmetros
                  e exemplos de requisição/resposta usando os campos reais
                - Para outras classes: campos e métodos relevantes

                Regras:
                - Use EXCLUSIVAMENTE as informações fornecidas. Não invente
                  campos, endpoints ou comportamentos.
                - A estrutura dos exemplos deve ser real; os valores podem
                  ser ilustrativos e plausíveis.
                - Resposta somente em Markdown, sem saudação nem comentário
                  final fora do conteúdo.
                """.formatted(
                pacote.nomePacote(),
                contextoArquitetura,
                resumoGlobalDePacotes,
                String.join("\n\n", pacote.resumosDasClasses())
        );

        return llmClient.gerarTexto(prompt);
    }

    // ── Etapa 3 ──────────────────────────────────────────────────────────────

    private String consolidar(
            String urlRepositorio,
            String contextoArquitetura,
            List<PacoteDocumentado> pacotes
    ) {
        String documentacoesPorPacote = pacotes.stream()
                .map(p -> "## Pacote: %s\n\n%s".formatted(p.nomePacote(), p.documentacao()))
                .collect(Collectors.joining("\n\n---\n\n"));

        String prompt = """
                Consolide as documentações de pacote abaixo em um documento
                técnico final e coerente sobre a API do repositório %s.

                CONTEXTO ARQUITETURAL (use para manter coerência):
                %s

                DOCUMENTAÇÕES POR PACOTE:
                %s

                Gere o documento final em Markdown com esta estrutura:

                # Documentação Técnica — %s

                ## Visão Arquitetural
                (síntese de como as camadas se relacionam, baseada no
                contexto arquitetural acima)

                ## Guia de Endpoints
                (consolide TODOS os endpoints de todos os pacotes de
                controller, com parâmetros e exemplos)

                ## Documentação Técnica
                (uma subseção por pacote, com o conteúdo já documentado
                acima — reorganize se necessário para melhorar a coerência,
                mas não invente informação nova)

                Regras:
                - Comece diretamente com "# Documentação Técnica — %s".
                - Não inclua saudação, introdução ou comentário fora do
                  documento. Blocos de código JSON são permitidos e esperados.
                - Não invente nenhuma informação que não esteja nas
                  documentações de pacote acima.
                """.formatted(
                urlRepositorio,
                contextoArquitetura,
                documentacoesPorPacote,
                urlRepositorio,
                urlRepositorio
        );

        return llmClient.gerarTexto(prompt);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String formatarCampos(List<CampoExtraido> campos) {
        if (campos.isEmpty()) return "(nenhum campo encontrado)";
        return campos.stream()
                .map(c -> "%s: %s".formatted(c.nome(), c.tipo()))
                .collect(Collectors.joining(", "));
    }

    private String formatarMetodos(List<MetodoExtraido> metodos) {
        return metodos.stream()
                .map(this::formatarMetodo)
                .collect(Collectors.joining("; "));
    }

    private String formatarMetodo(MetodoExtraido metodo) {
        String parametros = metodo.parametros().stream()
                .map(this::formatarParametro)
                .collect(Collectors.joining(", "));
        String anotacoes = metodo.anotacoes().isEmpty() ? "" : " " + metodo.anotacoes();
        return "%s(%s): %s%s".formatted(metodo.nome(), parametros, metodo.tipoRetorno(), anotacoes);
    }

    private String formatarParametro(ParametroExtraido parametro) {
        String anotacoes = parametro.anotacoes().isEmpty()
                ? ""
                : String.join(" ", parametro.anotacoes()) + " ";
        return "%s%s %s".formatted(anotacoes, parametro.tipo(), parametro.nome());
    }
}