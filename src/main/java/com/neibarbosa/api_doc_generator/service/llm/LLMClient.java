package com.neibarbosa.api_doc_generator.service.llm;
/**
 * Abstrai o acesso a um provedor de LLM, seguindo o mesmo Dependency
 * Inversion Principle já aplicado ao ProvedorRepositorio: o core da
 * aplicação depende apenas desta interface, nunca do SDK concreto de
 * um provedor específico (OpenAI, Anthropic, etc.).
 *
 * Trocar de provedor no futuro significa apenas criar um novo adapter
 * implementando esta interface — nenhuma linha do pipeline de geração
 * de documentação precisa mudar.
 */
public interface LLMClient {

    String gerarTexto(String promopt);
}
