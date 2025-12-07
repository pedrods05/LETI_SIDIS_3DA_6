package leti_sisdis_6.hapappointmentrecords.service.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.time.LocalDateTime;

import static leti_sisdis_6.hapappointmentrecords.config.RabbitMQConfig.CORRELATION_ID_HEADER;
import static org.assertj.core.api.Assertions.assertThat;

class AppointmentEventsListenerTest {

    private AppointmentEventsListener listener;

    @BeforeEach
    void setUp() {
        listener = new AppointmentEventsListener();
        MDC.clear();
    }

    @Test
    @DisplayName("Should process AppointmentCreatedEvent with correlation ID from header")
    void shouldProcessAppointmentCreatedEventWithCorrelationId() {
        // Given
        String correlationId = "test-correlation-123";
        AppointmentCreatedEvent event = new AppointmentCreatedEvent();
        event.setAppointmentId("APT001");
        event.setPatientId("PAT001");
        event.setPhysicianId("PHY001");
        event.setDateTime(LocalDateTime.now());

        MessageProperties props = new MessageProperties();
        props.setHeader(CORRELATION_ID_HEADER, correlationId);
        Message message = new Message(new byte[0], props);

        // When
        listener.onAppointmentCreated(event, message, correlationId);

        // Then - MDC should be cleaned after processing
        assertThat(MDC.get(CORRELATION_ID_HEADER)).isNull();
    }

    @Test
    @DisplayName("Should process AppointmentCreatedEvent with correlation ID from message properties")
    void shouldProcessAppointmentCreatedEventWithCorrelationIdFromMessage() {
        // Given
        String correlationId = "test-correlation-456";
        AppointmentCreatedEvent event = new AppointmentCreatedEvent();
        event.setAppointmentId("APT001");

        MessageProperties props = new MessageProperties();
        props.setHeader(CORRELATION_ID_HEADER, correlationId);
        Message message = new Message(new byte[0], props);

        // When
        listener.onAppointmentCreated(event, message, null);

        // Then - should extract from message
        assertThat(MDC.get(CORRELATION_ID_HEADER)).isNull();
    }

    @Test
    @DisplayName("Should process AppointmentCreatedEvent without correlation ID")
    void shouldProcessAppointmentCreatedEventWithoutCorrelationId() {
        // Given
        AppointmentCreatedEvent event = new AppointmentCreatedEvent();
        event.setAppointmentId("APT001");

        // When
        listener.onAppointmentCreated(event, null, null);

        // Then - should process without error
        assertThat(MDC.get(CORRELATION_ID_HEADER)).isNull();
    }

    @Test
    @DisplayName("Should handle null event gracefully in AppointmentCreated")
    void shouldHandleNullEventInAppointmentCreated() {
        // When/Then - should not throw exception
        listener.onAppointmentCreated(null, null, null);
    }

    @Test
    @DisplayName("Should process AppointmentUpdatedEvent")
    void shouldProcessAppointmentUpdatedEvent() {
        // Given
        String correlationId = "test-correlation-789";
        AppointmentUpdatedEvent event = new AppointmentUpdatedEvent();
        event.setAppointmentId("APT001");

        MessageProperties props = new MessageProperties();
        props.setHeader(CORRELATION_ID_HEADER, correlationId);
        Message message = new Message(new byte[0], props);

        // When
        listener.onAppointmentUpdated(event, message, correlationId);

        // Then
        assertThat(MDC.get(CORRELATION_ID_HEADER)).isNull();
    }

    @Test
    @DisplayName("Should handle null event in AppointmentUpdated")
    void shouldHandleNullEventInAppointmentUpdated() {
        // When/Then
        listener.onAppointmentUpdated(null, null, null);
    }

    @Test
    @DisplayName("Should process AppointmentCanceledEvent")
    void shouldProcessAppointmentCanceledEvent() {
        // Given
        String correlationId = "test-correlation-999";
        AppointmentCanceledEvent event = new AppointmentCanceledEvent();
        event.setAppointmentId("APT001");
        event.setReason("Patient request");

        MessageProperties props = new MessageProperties();
        props.setHeader(CORRELATION_ID_HEADER, correlationId);
        Message message = new Message(new byte[0], props);

        // When
        listener.onAppointmentCanceled(event, message, correlationId);

        // Then
        assertThat(MDC.get(CORRELATION_ID_HEADER)).isNull();
    }

    @Test
    @DisplayName("Should handle null event in AppointmentCanceled")
    void shouldHandleNullEventInAppointmentCanceled() {
        // When/Then
        listener.onAppointmentCanceled(null, null, null);
    }

    @Test
    @DisplayName("Should clean up MDC even when processing fails")
    void shouldCleanUpMDCEvenWhenProcessingFails() {
        // Given
        String correlationId = "test-correlation-fail";
        AppointmentCreatedEvent event = new AppointmentCreatedEvent();
        event.setAppointmentId("APT001");

        // When
        listener.onAppointmentCreated(event, null, correlationId);

        // Then - MDC should be cleaned up
        assertThat(MDC.get(CORRELATION_ID_HEADER)).isNull();
    }

    @Test
    @DisplayName("Should prefer header parameter over message properties")
    void shouldPreferHeaderParameterOverMessageProperties() {
        // Given
        String headerCorrelationId = "header-id";
        String messageCorrelationId = "message-id";

        AppointmentCreatedEvent event = new AppointmentCreatedEvent();
        event.setAppointmentId("APT001");

        MessageProperties props = new MessageProperties();
        props.setHeader(CORRELATION_ID_HEADER, messageCorrelationId);
        Message message = new Message(new byte[0], props);

        // When
        listener.onAppointmentCreated(event, message, headerCorrelationId);

        // Then - should use header parameter (verified by no exception)
        assertThat(MDC.get(CORRELATION_ID_HEADER)).isNull();
    }

    @Test
    @DisplayName("Should handle blank correlation ID in header")
    void shouldHandleBlankCorrelationIdInHeader() {
        // Given
        AppointmentCreatedEvent event = new AppointmentCreatedEvent();
        event.setAppointmentId("APT001");

        MessageProperties props = new MessageProperties();
        props.setHeader(CORRELATION_ID_HEADER, "   ");
        Message message = new Message(new byte[0], props);

        // When
        listener.onAppointmentCreated(event, message, "   ");

        // Then - should process without error
        assertThat(MDC.get(CORRELATION_ID_HEADER)).isNull();
    }

    @Test
    @DisplayName("Should use backward-compatible overload")
    void shouldUseBackwardCompatibleOverload() {
        // Given
        AppointmentCreatedEvent event = new AppointmentCreatedEvent();
        event.setAppointmentId("APT001");

        // When
        listener.onAppointmentCreated(event);

        // Then - should work without error
        assertThat(MDC.get(CORRELATION_ID_HEADER)).isNull();
    }

    @Test
    @DisplayName("Should extract correlation ID from message when header is null")
    void shouldExtractCorrelationIdFromMessageWhenHeaderIsNull() {
        // Given
        String correlationId = "message-correlation-id";
        AppointmentCreatedEvent event = new AppointmentCreatedEvent();
        event.setAppointmentId("APT001");

        MessageProperties props = new MessageProperties();
        props.setHeader(CORRELATION_ID_HEADER, correlationId);
        Message message = new Message(new byte[0], props);

        // When
        listener.onAppointmentCreated(event, message, null);

        // Then
        assertThat(MDC.get(CORRELATION_ID_HEADER)).isNull();
    }

    @Test
    @DisplayName("Should handle message with null properties")
    void shouldHandleMessageWithNullProperties() {
        // Given
        AppointmentCreatedEvent event = new AppointmentCreatedEvent();
        event.setAppointmentId("APT001");
        Message message = new Message(new byte[0], null);

        // When/Then - should not throw exception
        listener.onAppointmentCreated(event, message, null);
    }
}

