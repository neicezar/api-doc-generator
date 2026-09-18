package com.neibarbosa.api_doc_generator.provedor;


import java.io.IOException;
import java.io.InputStream;

/**
  Abstrai o acesso a um provedor de repositório de código (GitHub,
  GitLab, etc.), seguindo o Dependency Inversion Principle: o core
  da aplicação depende apenas desta interface, nunca de um SDK ou
  cliente HTTP específico de um provedor.

  Hoje só existe GitHubProvedor implementando esta interface, mas a
  arquitetura já está preparada para novos provedores (ex: GitLab
  self-hosted) sem precisar alterar nenhum código que já depende
  desta abstração — basta criar um novo adapter.
 */
public interface ProvedorRepositorio {
    /**
      Indica se este provedor sabe processar a URL informada.
      Usado pelo service para escolher, entre todos os provedores
      disponíveis, qual deles deve tratar a requisição.
     */

    boolean suporta(String urlRepositorio);

    /**
      Baixa o conteúdo compactado do repositório e devolve como
      stream, sem persistir nada em disco.

      @param urlRepositorio URL do repositório informada pelo usuário
      @param tokenAcesso    token de autenticação do provedor (pode ser nulo
                            para repositórios públicos)
     */

    InputStream baixarRepositorio(String urlRepositorio, String tokenAcesso) throws IOException;

}
