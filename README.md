# 📄 API Doc Generator

> Projeto de portfólio — gerador inteligente de documentação técnica para APIs Spring Boot, usando Java 17, Spring Boot 4.1.1, RabbitMQ, JavaParser e LLM (OpenAI).

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1.1-green?style=flat-square)
![Angular](https://img.shields.io/badge/Angular-17-red?style=flat-square)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.13-orange?style=flat-square)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?style=flat-square)

---

## 🎯 O que é

O **API Doc Generator** recebe a URL de um repositório público do GitHub, extrai a estrutura do código Java via análise estática (AST com JavaParser), e gera automaticamente uma documentação técnica completa em Markdown usando um pipeline de LLM — incluindo visão arquitetural, guia de endpoints com exemplos de requisição e resposta, e detalhamento por classe.

O processamento é 100% assíncrono: o usuário dispara a tarefa, acompanha o progresso em tempo real via SSE (Server-Sent Events) no frontend Angular, e ao final pode visualizar e baixar a documentação gerada.

---

## 🏗️ Arquitetura

```
┌─────────────────────────────────────────────────────────────────────┐
│  Frontend Angular 17                                                 │
│  FormularioComponent → ProgressoComponent → ResultadoComponent      │
└────────────────────────┬────────────────────────────────────────────┘
                         │ HTTP / SSE
┌────────────────────────▼────────────────────────────────────────────┐
│  Spring Boot 4.1.1 (Backend)                                         │
│                                                                      │
│  TarefaController                                                    │
│  ├── POST /api/tarefas        → cria tarefa, publica no RabbitMQ     │
│  ├── GET  /api/tarefas/{id}   → consulta status                      │
│  ├── GET  /api/tarefas/{id}/events    → SSE (progresso em tempo real)│
│  └── GET  /api/tarefas/{id}/documento → retorna o .md gerado         │
│                                                                      │
│  TarefaMensagemListener (RabbitMQ consumer)                          │
│  └── TarefaProcessamentoService                                      │
│      ├── GitHubProvedor → baixa o zipball sem tocar disco            │
│      ├── ExtratorDeCodigo → JavaParser (AST → ClasseExtraida)        │
│      └── GeradorDeDocumentacaoService → pipeline LLM (4 etapas)     │
└────────────────────────┬────────────────────────────────────────────┘
                         │
         ┌───────────────┼───────────────┐
         ▼               ▼               ▼
    PostgreSQL       RabbitMQ        OpenAI API
   (tarefas)      (tarefas.queue)   (gpt-4o-mini)
```

### Pipeline de geração de documentação (map-reduce em 4 etapas)

```
Etapa 0 → Detecta padrão arquitetural (MVC, hexagonal, etc.)
            ↓
Etapa 1 → Resume cada classe individualmente (map — 1 chamada por classe)
            ↓
Etapa 2 → Documenta cada pacote com contexto da arquitetura
            ↓
Etapa 3 → Consolida em documento final coerente (reduce)
            ↓
         Arquivo .md salvo em documentos-gerados/
```

---

## ⚙️ Decisões técnicas

### Por que RabbitMQ em vez de processar na thread da requisição?
A geração de documentação pode levar vários minutos (dependendo do tamanho do repositório). Processar na thread HTTP travaria a conexão e eventualmente daria timeout no cliente. Com RabbitMQ, a requisição retorna imediatamente com `202 Accepted`, e o processamento acontece de forma assíncrona num consumer dedicado.

### Por que SSE em vez de WebSocket?
A comunicação é **unidirecional** — o servidor notifica o cliente sobre mudanças de status, mas o cliente não precisa enviar nada de volta durante o processamento. SSE é a escolha mais simples e adequada para esse padrão (push-only do servidor). WebSocket adicionaria complexidade desnecessária para um fluxo que não requer bidirecionalidade.

### Por que JavaParser (AST) em vez de enviar o código bruto para a LLM?
Enviar o código-fonte bruto consumiria uma quantidade enorme de tokens (e custo) para repositórios médios, além de saturar a janela de contexto do modelo. O JavaParser extrai apenas a estrutura relevante (pacote, nome, camada, anotações, campos, métodos com seus parâmetros e anotações) — reduzindo o payload em ~95% sem perder a informação que importa para a documentação.

### Por que pipeline map-reduce em vez de uma única chamada?
Uma única chamada com todas as classes tende a degradar a qualidade em repositórios maiores ("lost in the middle" — o modelo perde coerência com contextos muito longos). O pipeline de 4 etapas mantém cada chamada focada, usa o contexto arquitetural detectado na Etapa 0 para dar coerência ao conjunto, e escala melhor para repositórios com dezenas de pacotes.

### Por que a interface `LLMClient` + `OpenAIClientAdapter`?
Dependency Inversion Principle (DIP) aplicado: o core da aplicação (`GeradorDeDocumentacaoService`) depende apenas da abstração `LLMClient`, nunca do SDK da OpenAI diretamente. Trocar de provedor (Anthropic, Gemini, modelo local) no futuro significa criar um novo `Adapter` — nenhuma linha do pipeline de geração muda.

O mesmo padrão foi aplicado ao `ProvedorRepositorio` → `GitHubProvedor`: o processamento de tarefas não conhece a API do GitHub, só a interface.

---

## 🚀 Como rodar

### Pré-requisitos
- Java 17+
- Docker Desktop (para PostgreSQL e RabbitMQ)
- Node.js 20+ e Angular CLI 17
- Chave de API da OpenAI

### 1. Suba a infraestrutura local
```bash
docker compose up -d
```
Isso sobe o PostgreSQL (porta 5432) e o RabbitMQ (porta 5672, painel em 15672).

### 2. Configure a chave da OpenAI
No IntelliJ, vá em **Run → Edit Configurations → Environment variables** e adicione:
```
OPENAI_API_KEY=sk-...
```

> ⚠️ Nunca adicione a chave diretamente no `application.yml` — a pasta `.idea/` está no `.gitignore`, então a Run Configuration é o lugar seguro para desenvolvimento local.

### 3. Rode o backend
```bash
./mvnw spring-boot:run
```
Ou execute a classe `ApiDocGeneratorApplication` diretamente pelo IntelliJ.

O backend sobe em `http://localhost:8080`.

### 4. Rode o frontend
```bash
cd api-doc-frontend
npm install
ng serve
```

Acesse `http://localhost:4200`.

---

## 📁 Estrutura de pacotes (backend)

```
com.neibarbosa.api_doc_generator
├── config/         → RabbitMQConfig, OpenAIConfig, WebConfig (CORS)
├── controller/     → TarefaController (POST, GET, SSE, documento)
├── dto/            → CriarTarefaRequest, TarefaResponse
├── entity/         → Tarefa, StatusTarefa
├── exception/      → GlobalExceptionHandler, ErroResponse
├── extraction/     → ExtratorDeCodigo, IdentificadorDeCamada,
│                     ClasseExtraida, MetodoExtraido, CampoExtraido,
│                     ParametroExtraido, CamadaClasse
├── messaging/      → TarefaMensagemListener, TarefaMensagemPublisher
├── provedor/       → ProvedorRepositorio (interface), ProvedorLocator
│   └── github/     → GitHubProvedor
├── repository/     → TarefaRepository
└── service/
    ├── TarefaService, TarefaProcessamentoService, SseEmitterService
    └── llm/        → LLMClient (interface), OpenAIClientAdapter,
                      GeradorDeDocumentacaoService, PacoteDocumentado
```

---

## 🛠️ Stack

| Camada | Tecnologia |
|---|---|
| Backend | Java 17, Spring Boot 4.1.1 |
| Mensageria | RabbitMQ 3.13 |
| Banco de dados | PostgreSQL 16 |
| Análise de código | JavaParser 3.26.2 |
| LLM | OpenAI Java SDK 4.5.0 (gpt-4o-mini) |
| Frontend | Angular 17 (standalone), ngx-markdown |
| Infra local | Docker Desktop |

---

## ✨ Funcionalidades

- ✅ Análise estática de repositórios Spring Boot públicos via GitHub API
- ✅ Identificação automática de camada (Controller, Service, Repository, Entity, DTO)
- ✅ Reconhece Java `record` como DTO
- ✅ Extrai campos, métodos, parâmetros e valores de anotações (ex: `@GetMapping("/greeting")`)
- ✅ Pipeline LLM map-reduce com detecção de arquitetura (MVC, hexagonal, etc.)
- ✅ Documentação com guia de endpoints e exemplos de requisição/resposta
- ✅ Progresso em tempo real via Server-Sent Events (SSE)
- ✅ Visualização e download do `.md` gerado
- ✅ Limite configurável de classes (padrão: 400) com mensagem de erro clara
- ✅ Deduplicação de classes em repositórios com múltiplos módulos
- ✅ Filtra arquivos de infraestrutura (Maven/Gradle wrapper, testes)
