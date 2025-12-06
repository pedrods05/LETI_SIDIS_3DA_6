package leti_sisdis_6.happhysicians.config;

import org.slf4j.MDC;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Value("${hap.rabbitmq.exchange:hap-exchange}")
    private String exchangeName;

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(exchangeName);
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        // Declara o exchange explicitamente
        admin.declareExchange(exchange());
        return admin;
    }

    @Bean
    public Jackson2JsonMessageConverter producerMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(producerMessageConverter());
        // Propagate correlation ID from MDC to AMQP message headers
        rabbitTemplate.setBeforePublishPostProcessors(message -> {
            String correlationId = MDC.get(CORRELATION_ID_HEADER);
            if (correlationId != null && !correlationId.isBlank()) {
                message.getMessageProperties().setHeader(CORRELATION_ID_HEADER, correlationId);
            }
            return message;
        });
        return rabbitTemplate;
    }
}

