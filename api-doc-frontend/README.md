# 🖥️ API Doc Generator — Frontend

> Interface Angular 17 para o [API Doc Generator](../README.md) — gera documentação técnica de APIs Spring Boot usando LLM, com acompanhamento do progresso em tempo real via Server-Sent Events (SSE).

---

## ✨ Funcionalidades

- **Formulário com validação** — aceita apenas URLs públicas do GitHub
- **Progresso em tempo real** — acompanha as etapas via SSE sem polling:
  `BAIXANDO → EXTRAINDO → GERANDO → CONCLUÍDO`
- **Documentação renderizada** — exibe o Markdown gerado com formatação completa (títulos, blocos de código, tabelas)
- **Download do arquivo** — baixa o `.md` gerado para usar no repositório da API
- **Copiar conteúdo** — copia o Markdown com um clique
- **Tema escuro** — interface pensada para desenvolvedores

---

## 🛠️ Stack

| Tecnologia | Versão |
|---|---|
| Angular | 17.3 (standalone components) |
| TypeScript | 5.4 |
| ngx-markdown | 17 |
| SCSS | — |

---

## 🚀 Como rodar

### Pré-requisitos
- Node.js 20+
- Angular CLI 17 (`npm install -g @angular/cli@17`)
- Backend rodando em `http://localhost:8080` ([instruções aqui](../README.md))

### Instalação e execução
```bash
npm install
ng serve
```

Acesse `http://localhost:4200`.

---

## 📁 Estrutura

```
src/app/
├── models/
│   └── tarefa.model.ts              → tipos: TarefaResponse, StatusTarefa
├── services/
│   └── tarefa.service.ts            → HTTP + EventSource (SSE)
├── components/
│   ├── formulario/                  → input de URL + validação
│   ├── progresso/                   → steps animados em tempo real
│   └── resultado/                   → Markdown renderizado + download
└── app.component.ts                 → máquina de estados da aplicação
```

### Máquina de estados

```
formulario → (POST /api/tarefas) → processando
                                       │
                    ┌──────────────────┤ SSE /api/tarefas/{id}/events
                    │  BAIXANDO        │
                    │  EXTRAINDO       │
                    │  GERANDO         │
                    └──────────────────┤
                                       ▼
                              concluido ──→ (GET /documento)
                              erro     ──→ mensagem de erro
```

### Por que `EventSource` em vez de `HttpClient` para o SSE?
`EventSource` é a API nativa do navegador para Server-Sent Events — uma conexão HTTP persistente de texto. O `HttpClient` do Angular é projetado para requisições HTTP convencionais (request → response), não para streams contínuos. Wrappamos o `EventSource` em um `Observable` para integrar naturalmente com o ecossistema reativo do Angular.

---

## 🔗 Repositório do backend

[api-doc-generator](../README.md) — Java 17, Spring Boot 4.1.1, RabbitMQ, JavaParser, OpenAI SDK
