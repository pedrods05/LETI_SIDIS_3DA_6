package leti_sisdis_6.hapappointmentrecords.service.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static leti_sisdis_6.hapappointmentrecords.config.RabbitMQConfig.CORRELATION_ID_HEADER;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentEventsPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AppointmentEventsPublisher publisher;

    private final String EXCHANGE_NAME = "test-exchange";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(publisher, "exchange", EXCHANGE_NAME);
        ReflectionTestUtils.setField(publisher, "recordEventsEnabled", true);
        MDC.clear();
    }

    @Test
    @DisplayName("Deve publicar AppointmentCreatedEvent com CorrelationID")
    void publishAppointmentCreated_Success() throws JsonProcessingException {
        AppointmentCreatedEvent event = new AppointmentCreatedEvent();
        event.setAppointmentId("app-1");

        byte[] jsonBytes = "{}".getBytes();
        when(objectMapper.writeValueAsBytes(event)).thenReturn(jsonBytes);

        MDC.put(CORRELATION_ID_HEADER, "corr-123");

        publisher.publishAppointmentCreated(event);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq(EXCHANGE_NAME), eq("appointment.created"), messageCaptor.capture());

        Message sentMessage = messageCaptor.getValue();
        assertNotNull(sentMessage);
        assertEquals("corr-123", sentMessage.getMessageProperties().getHeaders().get(CORRELATION_ID_HEADER));
        assertArrayEquals(jsonBytes, sentMessage.getBody());
    }

    @Test
    @DisplayName("Deve gerar novo CorrelationID se não existir no MDC")
    void publishAppointmentUpdated_GeneratesCorrelationId() throws JsonProcessingException {
        AppointmentUpdatedEvent event = new AppointmentUpdatedEvent();
        when(objectMapper.writeValueAsBytes(event)).thenReturn(new byte[0]);

        MDC.clear();

        publisher.publishAppointmentUpdated(event);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq(EXCHANGE_NAME), eq("appointment.updated"), messageCaptor.capture());

        String correlationId = (String) messageCaptor.getValue().getMessageProperties().getHeaders().get(CORRELATION_ID_HEADER);
        assertNotNull(correlationId);
        assertFalse(correlationId.isBlank());
    }

    @Test
    @DisplayName("Não deve publicar RecordCreated se flag estiver disabled")
    void publishAppointmentRecordCreated_Disabled() {
        ReflectionTestUtils.setField(publisher, "recordEventsEnabled", false);
        AppointmentRecordCreatedEvent event = new AppointmentRecordCreatedEvent();

        publisher.publishAppointmentRecordCreated(event);

        verifyNoInteractions(rabbitTemplate);
        verifyNoInteractions(objectMapper);
    }

    @Test
    @DisplayName("Deve publicar RecordCreated se flag estiver enabled")
    void publishAppointmentRecordCreated_Enabled() throws JsonProcessingException {
        ReflectionTestUtils.setField(publisher, "recordEventsEnabled", true);
        AppointmentRecordCreatedEvent event = new AppointmentRecordCreatedEvent();
        when(objectMapper.writeValueAsBytes(event)).thenReturn(new byte[0]);

        publisher.publishAppointmentRecordCreated(event);

        verify(rabbitTemplate).send(eq(EXCHANGE_NAME), eq("appointmentrecord.created"), any(Message.class));
    }

    @Test
    @DisplayName("Deve tratar exceção de serialização sem lançar erro (Log only)")
    void publish_ExceptionHandling() throws JsonProcessingException {
        AppointmentCanceledEvent event = new AppointmentCanceledEvent();
        when(objectMapper.writeValueAsBytes(event)).thenThrow(new RuntimeException("Serialization error"));

        assertDoesNotThrow(() -> publisher.publishAppointmentCanceled(event));

        verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
    }
}