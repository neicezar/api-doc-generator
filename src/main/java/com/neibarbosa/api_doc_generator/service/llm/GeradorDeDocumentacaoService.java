package com.neibarbosa.api_doc_generator.service.llm;

import com.neibarbosa.api_doc_generator.extraction.CampoExtraido;
import com.neibarbosa.api_doc_generator.extraction.ClasseExtraida;
import com.neibarbosa.api_doc_generator.extraction.MetodoExtraido;
import com.neibarbosa.api_doc_generator.extraction.ParametroExtraido;
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
                  Não invente métodos, campos ou comportamentos que não
                  estejam listados.
                - Responda APENAS com o resumo em si — sem saudação, sem
                  introdução ("Claro!", "Aqui está...", etc.), sem comentário
                  final, sem markdown de bloco de código.

                Pacote: %s
                Classe: %s
                Camada identificada: %s
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

    private String formatarCampos(List<CampoExtraido> campos) {
        if (campos.isEmpty()) {
            return "(nenhum campo encontrado)";
        }
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

        return "%s(%s): %s%s".formatted(
                metodo.nome(), parametros, metodo.tipoRetorno(), anotacoes
        );
    }

    private String formatarParametro(ParametroExtraido parametro) {
        String anotacoes = parametro.anotacoes().isEmpty()
                ? ""
                : String.join(" ", parametro.anotacoes()) + " ";

        return "%s%s %s".formatted(anotacoes, parametro.tipo(), parametro.nome());
    }

    /**
     * Etapa REDUCE: uma única chamada final, recebendo os resumos já
     * compactados (não o código bruto de novo), pedindo a
     * documentação consolidada do projeto inteiro.
     */
    private String reduzir(String urlRepositorio, List<String> resumos) {
        String prompt = """
                Você é um gerador de documentação técnica de APIs.
                Com base nos resumos de classes abaixo (extraídos do repositório %s),
                gere uma documentação completa em Markdown com três seções:

                1. "## Visão Arquitetural" — como as camadas se relacionam
                2. "## Guia de Endpoints" — para CADA endpoint exposto pelos
                   controllers, documente:
                   - método HTTP e path (ex: `GET /greeting`)
                   - parâmetros aceitos, com nome, tipo, se é obrigatório e
                     valor padrão quando houver
                   - um exemplo de requisição
                   - um exemplo de resposta em JSON, usando os campos reais
                     do objeto retornado
                3. "## Documentação Técnica" — detalhamento por classe, uma
                   subseção "### NomeDaClasse" para CADA classe listada abaixo,
                   incluindo seus campos (nome e tipo) quando houver

                Regras obrigatórias:
                - Use EXCLUSIVAMENTE as classes e informações listadas abaixo.
                  Nunca invente classes, métodos, campos ou endpoints que não
                  estejam nos resumos.
                - Sobre os exemplos: a ESTRUTURA deve ser sempre real (só os
                  campos e parâmetros que existem de fato nos resumos); apenas
                  os VALORES dentro dela podem ser ilustrativos e plausíveis.
                  Nunca acrescente um campo ao exemplo que não exista na classe.
                - Se não houver nenhum controller nos resumos, escreva na seção
                  de endpoints que o repositório não expõe endpoints REST, em
                  vez de inventar algum.
                - A resposta deve conter APENAS o Markdown da documentação,
                  começando diretamente com "# Documentação Técnica — %s".
                  Não inclua saudação, introdução, comentário final, nem
                  envolva a resposta inteira em um bloco de código markdown
                  (não use ``` no início/fim do documento). Blocos de código
                  para os exemplos de JSON/requisição são permitidos e
                  esperados.

                Resumos das classes (%d classes no total):
                %s
                """.formatted(
                urlRepositorio, urlRepositorio, resumos.size(), String.join("\n\n", resumos)
        );

        return llmClient.gerarTexto(prompt);
    }
}