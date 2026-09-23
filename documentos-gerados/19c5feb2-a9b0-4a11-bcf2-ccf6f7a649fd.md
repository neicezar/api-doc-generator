# Documentação Técnica da API de Serviço de Saudações

## Visão Arquitetural

A arquitetura da aplicação é baseada no padrão MVC (Model-View-Controller) e utiliza o framework Spring Boot para gerenciar as interações no ambiente RESTful. As principais componentes da arquitetura são:

- **Camada de Controle (Controller)**: A `GreetingController` é responsável por gerenciar as solicitações HTTP referentes a saudações. Ela recebe parâmetros de entrada do usuário, processa essas informações e retorna as respostas apropriadas ao cliente na forma de objetos JSON.

- **Camada de Aplicação (Application)**: A `RestServiceApplication` atua como ponto de entrada da aplicação. Ela inicializa o contexto do Spring e configura todos os componentes necessários. Esta classe gerencia as configurações do serviço, permite a execução da aplicação e ajuda na injeção de dependências.

A interação entre essas camadas é feita da seguinte maneira:
1. O cliente faz uma solicitação HTTP para o `GreetingController`.
2. O `GreetingController` processa a solicitação, utilizando a lógica de negócios apropriada e interagindo com os modelos, se necessário.
3. O controlador retorna uma resposta JSON (objeto `Greeting`) para o cliente, completando o ciclo de interação.

## Guia de Endpoints

### Endpoint de Saudação

- **GET /greeting**
  - **Descrição**: Retorna uma saudação personalizada com base no nome fornecido como parâmetro.
  - **Parâmetros**:
    - `name` (opcional): O nome da pessoa a ser saudada.
  - **Resposta**:
    - Código HTTP 200 (OK) com um objeto JSON contendo a mensagem de saudação.

### Exemplo de Uso

- Requisição: `GET /greeting?name=João`
- Resposta:
  ```json
  {
    "message": "Hello, João!"
  }
  ```

## Documentação Técnica

### Classe `GreetingController`

#### Descrição
A classe `GreetingController` é um controlador REST implementado para gerenciar as solicitações relacionadas às saudações. Ele é responsável por receber o parâmetro de nome do usuário e retornar uma resposta formatada com uma mensagem de saudação.

#### Métodos
- **`greeting(String name)`**: Método que lida com solicitações GET. Recebe um parâmetro opcional `name` e retorna um objeto `Greeting` que contém a saudação personalizada.

#### Anotações
- `@RestController`: Marca a classe como um controlador que retorna respostas diretamente no formato JSON.
- `@RequestMapping("/greeting")`: Define a rota base para as solicitações de saudação.

### Classe `RestServiceApplication`

#### Descrição
A classe `RestServiceApplication` é a classe principal da aplicação Spring Boot. Ela serve como ponto de entrada para inicializar o serviço REST.

#### Métodos
- **`public static void main(String[] args)`**: O método principal que inicia a aplicação Spring Boot.

#### Anotações
- `@SpringBootApplication`: Marcador que habilita a configuração automática, varredura de componentes e propriedades do Spring.

### Conclusão
Esta documentação fornece uma visão abrangente da API de Serviço de Saudações, abrangendo a arquitetura, endpoints disponíveis e detalhes técnicos sobre as classes envolvidas. Com isso, desenvolvedores e integradores podem entender como utilizar a API eficazmente.