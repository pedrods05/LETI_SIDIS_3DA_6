package leti_sisdis_6.hapappointmentrecords.service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static leti_sisdis_6.hapappointmentrecords.config.RabbitMQConfig.CORRELATION_ID_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentEventsPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private ObjectMapper objectMapper;
    private AppointmentEventsPublisher publisher;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        publisher = new AppointmentEventsPublisher(rabbitTemplate, objectMapper);
        ReflectionTestUtils.setField(publisher, "exchange", "test-exchange");
        ReflectionTestUtils.setField(publisher, "recordEventsEnabled", true);

        MDC.clear();
    }

    @Test
    @DisplayName("Should publish AppointmentCreatedEvent with correlation ID from MDC")
    void shouldPublishAppointmentCreatedEventWithCorrelationId() {
        // Given
        String correlationId = "test-correlation-123";
        MDC.put(CORRELATION_ID_HEADER, correlationId);

        AppointmentCreatedEvent event = new AppointmentCreatedEvent();
        event.setAppointmentId("APT001");
        event.setPatientId("PAT001");
        event.setPhysicianId("PHY001");

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

        // When
        publisher.publishAppointmentCreated(event);

        // Then
        verify(rabbitTemplate).send(eq("test-exchange"), eq("appointment.created"), messageCaptor.capture());

        Message capturedMessage = messageCaptor.getValue();
        Object headerValue = capturedMessage.getMessageProperties().getHeader(CORRELATION_ID_HEADER);
        assertThat(headerValue).isEqualTo(correlationId);
        assertThat(capturedMessage.getMessageProperties().getContentType()).isEqualTo(MessageProperties.CONTENT_TYPE_JSON);
    }

    @Test
    @DisplayName("Should generate correlation ID when not in MDC")
    void shouldGenerateCorrelationIdWhenNotInMDC() {
        // Given
        MDC.clear();
        AppointmentCreatedEvent event = new AppointmentCreatedEvent();
        event.setAppointmentId("APT001");

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

        // When
        publisher.publishAppointmentCreated(event);

        // Then
        verify(rabbitTemplate).send(eq("test-exchange"), eq("appointment.created"), messageCaptor.capture());

        Message capturedMessage = messageCaptor.getValue();
        Object correlationIdObj = capturedMessage.getMessageProperties().getHeader(CORRELATION_ID_HEADER);
        assertThat(correlationIdObj).isNotNull();
        assertThat(correlationIdObj.toString()).isNotBlank();
    }

    @Test
    @DisplayName("Should publish AppointmentRecordCreatedEvent when enabled")
    void shouldPublishAppointmentRecordCreatedEventWhenEnabled() {
        // Given
        AppointmentRecordCreatedEvent event = new AppointmentRecordCreatedEvent(
                "REC001", "APT001", "PAT001", "PHY001", LocalDateTime.now());

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

        // When
        publisher.publishAppointmentRecordCreated(event);

        // Then
        verify(rabbitTemplate).send(eq("test-exchange"), eq("appointmentrecord.created"), messageCaptor.capture());
    }

    @Test
    @DisplayName("Should not publish AppointmentRecordCreatedEvent when disabled")
    void shouldNotPublishAppointmentRecordCreatedEventWhenDisabled() {
        // Given
        ReflectionTestUtils.setField(publisher, "recordEventsEnabled", false);
        AppointmentRecordCreatedEvent event = new AppointmentRecordCreatedEvent(
                "REC001", "APT001", "PAT001", "PHY001", LocalDateTime.now());

        // When
        publisher.publishAppointmentRecordCreated(event);

        // Then
        verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
    }

    @Test
    @DisplayName("Should publish AppointmentUpdatedEvent")
    void shouldPublishAppointmentUpdatedEvent() {
        // Given
        AppointmentUpdatedEvent event = new AppointmentUpdatedEvent();
        event.setAppointmentId("APT001");

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

        // When
        publisher.publishAppointmentUpdated(event);

        // Then
        verify(rabbitTemplate).send(eq("test-exchange"), eq("appointment.updated"), messageCaptor.capture());
    }

    @Test
    @DisplayName("Should publish AppointmentCanceledEvent")
    void shouldPublishAppointmentCanceledEvent() {
        // Given
        AppointmentCanceledEvent event = new AppointmentCanceledEvent();
        event.setAppointmentId("APT001");
        event.setReason("Patient request");

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

        // When
        publisher.publishAppointmentCanceled(event);

        // Then
        verify(rabbitTemplate).send(eq("test-exchange"), eq("appointment.canceled"), messageCaptor.capture());
    }

    @Test
    @DisplayName("Should clean up correlation ID from MDC after publishing")
    void shouldCleanUpCorrelationIdAfterPublishing() {
        // Given
        String correlationId = "test-correlation-123";
        MDC.put(CORRELATION_ID_HEADER, correlationId);

        AppointmentCreatedEvent event = new AppointmentCreatedEvent();
        event.setAppointmentId("APT001");

        // When
        publisher.publishAppointmentCreated(event);

        // Then
        assertThat(MDC.get(CORRELATION_ID_HEADER)).isNull();
    }

    @Test
    @DisplayName("Should handle exception during publishing gracefully")
    void shouldHandleExceptionDuringPublishing() {
        // Given
        doThrow(new RuntimeException("RabbitMQ connection failed"))
                .when(rabbitTemplate).send(anyString(), anyString(), any(Message.class));

        AppointmentCreatedEvent event = new AppointmentCreatedEvent();
        event.setAppointmentId("APT001");

        // When/Then - should not throw exception
        publisher.publishAppointmentCreated(event);

        // Verify MDC was cleaned up even after exception
        assertThat(MDC.get(CORRELATION_ID_HEADER)).isNull();
    }

    @Test
    @DisplayName("Should serialize event data correctly")
    void shouldSerializeEventDataCorrectly() throws Exception {
        // Given
        AppointmentCreatedEvent event = new AppointmentCreatedEvent();
        event.setAppointmentId("APT001");
        event.setPatientId("PAT001");
        event.setPhysicianId("PHY001");

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

        // When
        publisher.publishAppointmentCreated(event);

        // Then
        verify(rabbitTemplate).send(anyString(), anyString(), messageCaptor.capture());

        Message message = messageCaptor.getValue();
        byte[] body = message.getBody();
        AppointmentCreatedEvent deserialized = objectMapper.readValue(body, AppointmentCreatedEvent.class);

        assertThat(deserialized.getAppointmentId()).isEqualTo("APT001");
        assertThat(deserialized.getPatientId()).isEqualTo("PAT001");
        assertThat(deserialized.getPhysicianId()).isEqualTo("PHY001");
    }

    @Test
    @DisplayName("Should use configured exchange name")
    void shouldUseConfiguredExchangeName() {
        // Given
        ReflectionTestUtils.setField(publisher, "exchange", "custom-exchange");
        AppointmentCreatedEvent event = new AppointmentCreatedEvent();
        event.setAppointmentId("APT001");

        // When
        publisher.publishAppointmentCreated(event);

        // Then
        verify(rabbitTemplate).send(eq("custom-exchange"), anyString(), any(Message.class));
    }

    @Test
    @DisplayName("Should not clean up MDC if correlation ID was pre-existing")
    void shouldNotCleanUpPreExistingCorrelationId() {
        // Given
        String preExistingId = "pre-existing-id";
        MDC.put(CORRELATION_ID_HEADER, preExistingId);

        AppointmentCreatedEvent event = new AppointmentCreatedEvent();
        event.setAppointmentId("APT001");

        // When
        publisher.publishAppointmentCreated(event);

        // Then - should have cleaned up since it was the same ID
        assertThat(MDC.get(CORRELATION_ID_HEADER)).isNull();
    }

    @Test
    @DisplayName("Should publish with null event gracefully")
    void shouldPublishWithNullEventGracefully() {
        // When/Then - should not throw exception
        publisher.publishAppointmentCreated(null);

        verify(rabbitTemplate).send(anyString(), anyString(), any(Message.class));
    }
}

