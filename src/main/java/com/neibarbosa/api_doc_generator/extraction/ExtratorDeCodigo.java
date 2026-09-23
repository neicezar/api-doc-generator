package com.neibarbosa.api_doc_generator.extraction;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.stream.Collectors;

/**
 * Lê o repositório compactado (stream vindo do ProvedorRepositorio,
 * sem nunca tocar disco) e extrai, para cada arquivo .java, uma
 * representação estruturada via AST (JavaParser) — em vez de mandar
 * código-fonte bruto para a LLM na Fase 5.
 */
@Component
@RequiredArgsConstructor
public class ExtratorDeCodigo {

    private final IdentificadorDeCamada identificadorDeCamada;

    public List<ClasseExtraida> extrair(InputStream zipStream) throws IOException {
        List<ClasseExtraida> classesExtraidas = new ArrayList<>();

        try (ZipInputStream zis = new ZipInputStream(zipStream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String nomeArquivo = entry.getName();

                // Ignora diretórios, arquivos não-Java, e classes de teste
                // (não interessam para a documentação da API pública).
                boolean ehArquivoDeTeste = nomeArquivo.contains("/test/") || nomeArquivo.endsWith("Test.java");
                if (entry.isDirectory() || !nomeArquivo.endsWith(".java") || ehArquivoDeTeste) {
                    continue;
                }

                String codigoFonte = lerConteudoDoArquivo(zis);
                extrairClasse(codigoFonte).ifPresent(classesExtraidas::add);
            }
        }

        return deduplicarPorNomeCompleto(classesExtraidas);
    }

    /**
     * Alguns repositórios (principalmente tutoriais, como os "guides"
     * do próprio Spring) mantêm mais de uma pasta com cópias dos
     * mesmos arquivos-fonte (ex: "initial/" e "complete/"). Isso faz
     * a mesma classe ser encontrada mais de uma vez — o que é correto
     * do ponto de vista do repositório, mas indesejável na
     * documentação final, que não deveria repetir a mesma classe.
     * Mantém a primeira ocorrência encontrada, descartando as demais.
     */
    private List<ClasseExtraida> deduplicarPorNomeCompleto(List<ClasseExtraida> classes) {
        Map<String, ClasseExtraida> vistos = new LinkedHashMap<>();
        for (ClasseExtraida classe : classes) {
            String nomeCompleto = classe.nomePacote() + "." + classe.nomeClasse();
            vistos.putIfAbsent(nomeCompleto, classe);
        }
        return new ArrayList<>(vistos.values());
    }

    private String lerConteudoDoArquivo(ZipInputStream zis) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int bytesLidos;
        while ((bytesLidos = zis.read(chunk)) != -1) {
            buffer.write(chunk, 0, bytesLidos);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private Optional<ClasseExtraida> extrairClasse(String codigoFonte) {
        ParseResult<CompilationUnit> resultado = new JavaParser().parse(codigoFonte);

        // Um arquivo que não parseia (código incompleto, sintaxe de uma
        // versão de Java não suportada, etc.) é ignorado silenciosamente
        // em vez de derrubar o processamento do repositório inteiro.
        if (resultado.getResult().isEmpty()) {
            return Optional.empty();
        }

        CompilationUnit unidadeCompilacao = resultado.getResult().get();
        String nomePacote = unidadeCompilacao.getPackageDeclaration()
                .map(pd -> pd.getNameAsString())
                .orElse("");

        List<TypeDeclaration<?>> tipos = unidadeCompilacao.getTypes();
        if (tipos.isEmpty()) {
            return Optional.empty();
        }

        // Pega o primeiro tipo de nível superior do arquivo. Não depende
        // de nome de arquivo/storage (ao contrário de getPrimaryType()),
        // o que importa aqui porque parseamos a partir de uma String pura,
        // sem nenhuma informação de path associada ao CompilationUnit.
        TypeDeclaration<?> tipo = tipos.get(0);

        return Optional.of(extrairDados(tipo, nomePacote));
    }

    private ClasseExtraida extrairDados(TypeDeclaration<?> tipo, String nomePacote) {
        String nomeClasse = tipo.getNameAsString();
        boolean ehRecord = tipo instanceof RecordDeclaration;

        List<String> anotacoesDeClasse = tipo.getAnnotations().stream()
                .map(AnnotationExpr::getNameAsString)
                .toList();

        List<CampoExtraido> campos = extrairCampos(tipo);

        boolean possuiCampoComValidacao = tipo.getFields().stream()
                .flatMap(campo -> campo.getAnnotations().stream())
                .map(AnnotationExpr::getNameAsString)
                .anyMatch(identificadorDeCamada::ehAnotacaoDeValidacao);

        CamadaClasse camada = identificadorDeCamada.identificar(
                nomePacote, nomeClasse, anotacoesDeClasse, ehRecord, possuiCampoComValidacao
        );

        List<MetodoExtraido> metodos = tipo.getMethods().stream()
                .filter(MethodDeclaration::isPublic)
                .map(this::extrairMetodo)
                .toList();

        return new ClasseExtraida(nomePacote, nomeClasse, camada, anotacoesDeClasse, campos, metodos);
    }

    /**
     * Extrai os campos da classe — tratando records de forma diferente
     * de classes comuns, porque no JavaParser os componentes de um
     * record (ex: "record Greeting(long id, String content)") ficam
     * guardados como parâmetros do record, não como FieldDeclaration
     * no corpo, ao contrário de uma classe tradicional com campos
     * declarados explicitamente.
     */
    private List<CampoExtraido> extrairCampos(TypeDeclaration<?> tipo) {
        if (tipo instanceof RecordDeclaration record) {
            return record.getParameters().stream()
                    .map(p -> new CampoExtraido(p.getNameAsString(), p.getType().asString()))
                    .toList();
        }

        return tipo.getFields().stream()
                .flatMap(campo -> campo.getVariables().stream())
                .map(variavel -> new CampoExtraido(variavel.getNameAsString(), variavel.getType().asString()))
                .toList();
    }

    private MetodoExtraido extrairMetodo(MethodDeclaration metodo) {
        List<ParametroExtraido> parametros = metodo.getParameters().stream()
                .map(p -> new ParametroExtraido(
                        p.getNameAsString(),
                        p.getType().asString(),
                        p.getAnnotations().stream().map(this::formatarAnotacao).toList()
                ))
                .toList();

        // Usa formatarAnotacao (não só o nome) para capturar o valor de
        // anotações de mapping, ex: "GetMapping(/greeting)" em vez de só
        // "GetMapping" — essencial para o "Guia de Endpoints" da
        // documentação final conseguir listar o path real, sem precisar
        // que a LLM adivinhe.
        List<String> anotacoes = metodo.getAnnotations().stream()
                .map(this::formatarAnotacao)
                .toList();

        return new MetodoExtraido(
                metodo.getNameAsString(),
                parametros,
                metodo.getType().asString(),
                anotacoes
        );
    }

    private String formatarAnotacao(AnnotationExpr anotacao) {
        if (anotacao.isSingleMemberAnnotationExpr()) {
            String valor = anotacao.asSingleMemberAnnotationExpr().getMemberValue().toString();
            return "%s(%s)".formatted(anotacao.getNameAsString(), valor);
        }

        if (anotacao.isNormalAnnotationExpr()) {
            String pares = anotacao.asNormalAnnotationExpr().getPairs().stream()
                    .map(par -> "%s=%s".formatted(par.getNameAsString(), par.getValue().toString()))
                    .collect(Collectors.joining(", "));
            return "%s(%s)".formatted(anotacao.getNameAsString(), pares);
        }

        return anotacao.getNameAsString();
    }
}