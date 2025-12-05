package leti_sisdis_6.hapappointmentrecords.service.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.slf4j.MDC;
import leti_sisdis_6.hapappointmentrecords.config.RabbitMQConfig;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AppointmentEventPublisherTest {

    private RabbitTemplate rabbitTemplate;
    private AppointmentEventPublisher publisher;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        rabbitTemplate = mock(RabbitTemplate.class);
        publisher = new AppointmentEventPublisher(rabbitTemplate);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void publishWithCorrelationId_setsHeader() throws Exception {
        // Executar apenas este teste: mvn -Dtest=AppointmentEventPublisherTest test
        // O teste usa Mockito para capturar o MessagePostProcessor e validar o header X-Correlation-Id

        MDC.put(RabbitMQConfig.CORRELATION_ID_HEADER, "cid-123");
        ArgumentCaptor<MessagePostProcessor> mppCaptor = ArgumentCaptor.forClass(MessagePostProcessor.class);

        Object payload = new Object();
        publisher.publishAppointmentCreated(payload);

        verify(rabbitTemplate).convertAndSend(eq(publisher.getExchange()), eq(publisher.getRoutingKey()), eq(payload), mppCaptor.capture());

        Message msg = new Message(new byte[0], new MessageProperties());
        Message processed = (Message) mppCaptor.getValue().postProcessMessage(msg);
        assertEquals("cid-123", processed.getMessageProperties().getHeaders().get(RabbitMQConfig.CORRELATION_ID_HEADER));
    }

    @Test
    void publishWithoutCorrelationId_callsSimpleSend() {
        // sem MDC
        Object payload = new Object();
        publisher.publishAppointmentCreated(payload);

        verify(rabbitTemplate).convertAndSend(eq(publisher.getExchange()), eq(publisher.getRoutingKey()), eq(payload));
        // e nenhuma chamada com MessagePostProcessor
        verify(rabbitTemplate, never()).convertAndSend(eq(publisher.getExchange()), eq(publisher.getRoutingKey()), eq(payload), any(MessagePostProcessor.class));
    }
}
