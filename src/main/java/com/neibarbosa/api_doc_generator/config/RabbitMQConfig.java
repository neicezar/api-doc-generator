package com.neibarbosa.api_doc_generator.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configura a infraestrutura de mensageria: a exchange e a fila que
 * desacoplam a criação de uma tarefa (feita no TarefaController/Service)
 * do seu processamento real (feito pelo TarefaMensagemListener).
 *
 * Usamos DirectExchange porque o cenário é simples: uma mensagem
 * publicada com a routing key exata deve ir para exatamente uma
 * fila — é o modelo clássico de fila de tarefas (work queue), sem
 * necessidade dos recursos de roteamento mais complexos de uma
 * TopicExchange ou de broadcast de uma FanoutExchange.
 *
 * Nota: hoje a mensagem publicada é apenas uma String (o código da
 * tarefa), então usamos o RabbitTemplate/MessageConverter padrão do
 * Spring Boot (auto-configurado), que já lida com String nativamente.
 * Um Jackson2JsonMessageConverter customizado foi removido daqui por
 * incompatibilidade com o Jackson 3 que vem por padrão no Spring Boot 4
 * — se um dia a mensagem crescer para um objeto estruturado, isso
 * precisará ser revisitado (ou com Jackson 2 explícito no classpath,
 * ou aguardando o spring-amqp suportar Jackson 3 nativamente).
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "tarefas.exchange";
    public static final String QUEUE = "tarefas.queue";
    public static final String ROUTING_KEY = "tarefas.processar";

    @Bean
    public DirectExchange tarefasExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public Queue tarefasQueue() {
        // durable = true: a fila sobrevive a um restart do RabbitMQ,
        // e mensagens não confirmadas (ack) não se perdem.
        return new Queue(QUEUE, true);
    }

    @Bean
    public Binding binding(Queue tarefasQueue, DirectExchange tarefasExchange) {
        return BindingBuilder.bind(tarefasQueue)
                .to(tarefasExchange)
                .with(ROUTING_KEY);
    }
}