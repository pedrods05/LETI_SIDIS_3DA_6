package leti_sisdis_6.hapappointmentrecords.service.event;

import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import leti_sisdis_6.hapappointmentrecords.config.RabbitMQConfig;

@Component
public class AppointmentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${hap.rabbitmq.exchange:hap-appointmentrecords-exchange}")
    private String exchange;

    @Value("${hap.rabbitmq.routing-key:appointment.record.created}")
    private String routingKey;

    public AppointmentEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishAppointmentCreated(Object payload) {
        // Optionally attach correlation id from MDC
        String correlationId = MDC.get(RabbitMQConfig.CORRELATION_ID_HEADER);
        if (correlationId != null) {
            rabbitTemplate.convertAndSend(exchange, routingKey, payload, message -> {
                message.getMessageProperties().setHeader(RabbitMQConfig.CORRELATION_ID_HEADER, correlationId);
                return message;
            });
        } else {
            rabbitTemplate.convertAndSend(exchange, routingKey, payload);
        }
    }

    // getters adicionados para facilitar testes
    public String getExchange() {
        return this.exchange;
    }

    public String getRoutingKey() {
        return this.routingKey;
    }

    // Dica: para testes unitários execute: mvn test (ou ./gradlew test)
    // Para teste integrado com RabbitMQ local:
    // 1) docker run -d --name rabbit -p 5672:5672 -p 15672:15672 rabbitmq:3-management
    // 2) ajustar application.properties (spring.rabbitmq.*) e iniciar a aplicação
}
