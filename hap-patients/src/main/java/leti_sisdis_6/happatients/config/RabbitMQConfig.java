package leti_sisdis_6.happatients.config;

import org.slf4j.MDC;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Value("${hap.rabbitmq.exchange}")
    private String exchangeName;

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(exchangeName);
    }

    @Bean
    public Jackson2JsonMessageConverter producerMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(producerMessageConverter());
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
