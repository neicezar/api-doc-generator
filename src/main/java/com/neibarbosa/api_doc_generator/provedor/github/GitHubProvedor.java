package com.neibarbosa.api_doc_generator.provedor.github;

import com.neibarbosa.api_doc_generator.provedor.ProvedorRepositorio;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GitHubProvedor implements ProvedorRepositorio {

    // Extrai "owner" e "repositorio" de uma URL como
    // https://github.com/owner/repositorio
    private static final Pattern URL_GITHUB = Pattern.compile(
            "^https://github\\.com/([A-Za-z0-9_.-]+)/([A-Za-z0-9_.-]+)/?$"
    );

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    public boolean suporta(String urlRepositorio){
        return urlRepositorio != null && urlRepositorio.contains("github.com");
    }

    @Override
    public InputStream baixarRepositorio(String urlRepositorio, String tokenAcesso) throws IOException {
        Matcher matcher = URL_GITHUB.matcher(urlRepositorio);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "URL do GitHub em formato inesperado: " + urlRepositorio
            );
        }

        String owner = matcher.group(1);
        String repositorio = matcher.group(2);

        String zipballUrl = "https://api.github.com/repos/" + owner + "/" + repositorio + "/zipball/HEAD";

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(zipballUrl))
                .header("Accept", "application/vnd.github+json")
                .GET();

        if (tokenAcesso != null && !tokenAcesso.isBlank()) {
            requestBuilder.header("Authorization", "Bearer " + tokenAcesso);
        }

        try {
            HttpResponse<InputStream> response = httpClient.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofInputStream()
            );

            if (response.statusCode() != 200) {
                throw new IOException(
                        "Falha ao baixar repositório do GitHub. Status HTTP: " + response.statusCode()
                );
            }

            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Download do repositório interrompido", e);
        }
    }

}
