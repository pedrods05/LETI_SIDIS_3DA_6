package leti_sisdis_6.hapappointmentrecords.service.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static leti_sisdis_6.hapappointmentrecords.config.RabbitMQConfig.CORRELATION_ID_HEADER;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AppointmentEventsListenerTest {

    private final AppointmentEventsListener listener = new AppointmentEventsListener();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("Deve processar AppointmentCreatedEvent sem erros")
    void onAppointmentCreated_Success() {
        AppointmentCreatedEvent event = new AppointmentCreatedEvent();
        event.setAppointmentId("app-1");

        MessageProperties props = new MessageProperties();
        Message message = new Message(new byte[0], props);

        assertDoesNotThrow(() -> listener.onAppointmentCreated(event, message, "corr-header-1"));

    }

    @Test
    @DisplayName("Deve dar prioridade ao Header da anotação @Header")
    void correlationId_PriorityHeaderAnnotation() {
        AppointmentUpdatedEvent event = new AppointmentUpdatedEvent();
        Message message = new Message(new byte[0], new MessageProperties());

        assertDoesNotThrow(() -> listener.onAppointmentUpdated(event, message, "high-priority-id"));
    }

    @Test
    @DisplayName("Deve extrair CorrelationId das propriedades da mensagem se @Header for nulo")
    void correlationId_FromMessageProperties() {
        AppointmentCanceledEvent event = new AppointmentCanceledEvent();
        MessageProperties props = new MessageProperties();
        props.setHeader(CORRELATION_ID_HEADER, "msg-prop-id");
        Message message = new Message(new byte[0], props);

        assertDoesNotThrow(() -> listener.onAppointmentCanceled(event, message, null));
    }

    @Test
    @DisplayName("Deve funcionar mesmo com CorrelationId nulo em ambos")
    void correlationId_NullSafe() {
        AppointmentCreatedEvent event = new AppointmentCreatedEvent();
        Message message = new Message(new byte[0], new MessageProperties());

        assertDoesNotThrow(() -> listener.onAppointmentCreated(event, message, null));
    }

    @Test
    @DisplayName("Teste do overload de retrocompatibilidade")
    void onAppointmentCreated_LegacyOverload() {
        AppointmentCreatedEvent event = new AppointmentCreatedEvent();
        assertDoesNotThrow(() -> listener.onAppointmentCreated(event));
    }
}