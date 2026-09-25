# Documentação Técnica — https://github.com/spring-guides/gs-rest-service

## Visão Arquitetural
O projeto segue o padrão arquitetural MVC (Model-View-Controller), onde as responsabilidades de gerenciamento de requisições, processamento de dados e representação de informações são distribuídas entre diferentes camadas. A camada de apresentação é responsável por lidar com as requisições dos usuários e retornar as respostas adequadas, utilizando um controlador para processar a lógica da aplicação e um objeto de transferência de dados (DTO) para encapsular as informações. A classe principal inicializa o contexto do Spring Boot, preparando a aplicação para receber e processar essas requisições.

## Guia de Endpoints
### Endpoint: GET /greeting
- **Descrição:** Retorna uma saudação personalizada.
- **Parâmetros:**
  - `name` (opcional): Nome a ser incluído na saudação. O valor padrão é "World".
- **Exemplo de Requisição:**
  ```
  GET /greeting?name=John
  ```

- **Exemplo de Resposta:**
  ```json
  {
    "id": 1,
    "content": "Hello, John!"
  }
  ```

- **Exemplo de Requisição (sem parâmetro):**
  ```
  GET /greeting
  ```

- **Exemplo de Resposta (com valor padrão):**
  ```json
  {
    "id": 2,
    "content": "Hello, World!"
  }
  ```

## Documentação Técnica

### Pacote: com.example.restservice

#### Descrição do Pacote
O pacote `com.example.restservice` é parte da camada de apresentação de uma API RESTful baseada no padrão arquitetural MVC. Ele é responsável pela gestão de requisições e respostas relacionadas a saudações.

#### Classes

##### Greeting (DTO)
A classe `Greeting` representa um objeto de transferência de dados que encapsula as informações de uma saudação.

**Campos:**
- `long id`: Identificador único da saudação.
- `String content`: Mensagem da saudação.

##### GreetingController (CONTROLLER)
A classe `GreetingController` gerencia as requisições HTTP para gerar saudações personalizadas.

**Endpoints:**

- **GET /greeting**
  - **Descrição:** Retorna uma saudação personalizada.
  - **Parâmetros:**
    - `name` (opcional): Nome a ser incluído na saudação. O valor padrão é "World".
  - **Exemplo de Requisição:**
    ```
    GET /greeting?name=John
    ```

  - **Exemplo de Resposta:**
    ```json
    {
      "id": 1,
      "content": "Hello, John!"
    }
    ```

  - **Exemplo de Requisição (sem parâmetro):**
    ```
    GET /greeting
    ```

  - **Exemplo de Resposta (com valor padrão):**
    ```json
    {
      "id": 2,
      "content": "Hello, World!"
    }
    ```

##### RestServiceApplication (DESCONHECIDA)
A classe `RestServiceApplication` é a classe principal da aplicação.

**Métodos:**

- `public static void main(String[] args)`: Ponto de entrada da aplicação, que inicializa o contexto do Spring Boot e inicia o servidor. Não há campos definidos nesta classe.