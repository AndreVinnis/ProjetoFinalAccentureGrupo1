package br.accenture.ProjetoFinalAccentureGrupo1.notification.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
// Autor: André Vinícius Barros Macambira
public class RabbitConfig {

    public static final String EMAIL_QUEUE = "email.queue";

    // Fila que persiste a restart
    @Bean
    public Queue emailQueue() {
        return new Queue(EMAIL_QUEUE, true);
    }

    // Converte mensagem como JSON
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
