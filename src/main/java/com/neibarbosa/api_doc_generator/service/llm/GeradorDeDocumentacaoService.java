package com.neibarbosa.api_doc_generator.service.llm;

import com.neibarbosa.api_doc_generator.extraction.ClasseExtraida;
import com.neibarbosa.api_doc_generator.extraction.MetodoExtraido;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Orquestra a geração da documentação final usando o padrão map-reduce
 * definido no planejamento do projeto:
 *
 * MAP: cada classe extraída vira um resumo curto e independente
 * (evita mandar o repositório inteiro numa única chamada — problema
 * de "lost in the middle" em contextos muito longos).
 *
 * REDUCE: os resumos (já compactos) são consolidados numa única
 * chamada final, que gera a documentação completa em Markdown.
 */

@Service
@RequiredArgsConstructor
public class GeradorDeDocumentacaoService {
    private final LLMClient llmClient;

    public String gerarDocumentacao(String urlRepositorio, List<ClasseExtraida> classes) {
        List<String> resumos = mapear(classes);
        return reduzir(urlRepositorio, resumos);
    }

    /**
     * Etapa MAP: uma chamada de LLM por classe, gerando um resumo
     * curto e independente do que aquela classe faz.
     */
    private List<String> mapear(List<ClasseExtraida> classes) {
        return classes.stream()
                .map(this::resumirClasse)
                .collect(Collectors.toList());
    }

    private String resumirClasse(ClasseExtraida classe) {
        String prompt = """
                Resuma em no máximo 3 frases o que a classe Java abaixo faz,
                de forma técnica e objetiva, para uso em documentação de API.
                Não repita a assinatura, apenas explique o propósito.
                
                Pacote: %s
                Classe: %s
                Camada identificada: %s
                Anotações: %s
                Métodos públicos: %s
                """.formatted(
                classe.nomePacote(),
                classe.nomeClasse(),
                classe.camada(),
                classe.anotacoesDeClasse(),
                formatarMetodos(classe.metodos())

        );

        return "### %s (%s)\n%s".formatted(
                classe.nomeClasse(), classe.camada(), llmClient.gerarTexto(prompt)
        );
    }

    private String formatarMetodos(List<MetodoExtraido> metodos) {
        return metodos.stream()
                .map(m -> "%s(%s): %s".formatted(m.nome(), String.join(", ", m.parametros()), m.tipoRetorno()))
                .collect(Collectors.joining("; "));
    }
        /**
         * Etapa REDUCE: uma única chamada final, recebendo os resumos já
         * compactados (não o código bruto de novo), pedindo a
         * documentação consolidada do projeto inteiro.
         */
    private String reduzir(String urlRepositorio, List<String> resumos){
        String prompt = """
                Você é um gerador de documentação técnica de APIs.
                Com base nos resumos de classes abaixo (extraídos do repositório %s),
                gere uma documentação completa em Markdown com três seções:
                
                1. "## Visão Arquitetural" - como as camadas se relacionam
                2. "## Guia de Endpoints" - endpoints expostos pelos controllers, se houver
                3. "## Documentação Técnica" - detalhamento por classe
                
                
                Resumos das classes:
                %s
                """.formatted(urlRepositorio, String.join("\n\n", resumos));

        return llmClient.gerarTexto(prompt);
    }
}
