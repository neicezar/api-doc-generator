package com.neibarbosa.api_doc_generator.extraction;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

        return classesExtraidas;
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

        return unidadeCompilacao.findFirst(ClassOrInterfaceDeclaration.class).map(tipo -> {
            String nomeClasse = tipo.getNameAsString();

            List<String> anotacoesDeClasse = tipo.getAnnotations().stream()
                    .map(AnnotationExpr::getNameAsString)
                    .toList();

            boolean possuiCampoComValidacao = tipo.getFields().stream()
                    .flatMap(campo -> campo.getAnnotations().stream())
                    .map(AnnotationExpr::getNameAsString)
                    .anyMatch(identificadorDeCamada::ehAnotacaoDeValidacao);

            CamadaClasse camada = identificadorDeCamada.identificar(
                    nomePacote, nomeClasse, anotacoesDeClasse, possuiCampoComValidacao
            );

            List<MetodoExtraido> metodos = tipo.getMethods().stream()
                    .filter(MethodDeclaration::isPublic)
                    .map(this::extrairMetodo)
                    .toList();

            return new ClasseExtraida(nomePacote, nomeClasse, camada, anotacoesDeClasse, metodos);
        });
    }

    private MetodoExtraido extrairMetodo(MethodDeclaration metodo) {
        List<String> parametros = metodo.getParameters().stream()
                .map(p -> p.getType().asString() + " " + p.getNameAsString())
                .toList();

        List<String> anotacoes = metodo.getAnnotations().stream()
                .map(AnnotationExpr::getNameAsString)
                .toList();

        return new MetodoExtraido(
                metodo.getNameAsString(),
                parametros,
                metodo.getType().asString(),
                anotacoes
        );
    }
}