package leti_sisdis_6.hapappointmentrecords.service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Counted;
import leti_sisdis_6.hapappointmentrecords.model.EventStoreEntry;
import leti_sisdis_6.hapappointmentrecords.repository.EventStoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.Instant;

import static leti_sisdis_6.hapappointmentrecords.config.RabbitMQConfig.CORRELATION_ID_HEADER;

/**
 * Listens to appointment events from physicians service for logging/monitoring purposes.
 * AppointmentRecords does NOT store appointment data locally - it queries physicians service when needed.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AppointmentEventsListener {

    private final EventStoreRepository eventStoreRepository;
    private final ObjectMapper objectMapper;
    private final SagaCoordinator sagaCoordinator;

    @Value("${hap.saga.compensation.enabled:true}")
    private boolean compensationEnabled;

    // Backward-compatible overload used by existing tests
    public void onAppointmentCreated(AppointmentCreatedEvent event) {
        onAppointmentCreated(event, null, null);
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "q.appointmentrecords.projection.${spring.profiles.active}", durable = "true"),
            exchange = @Exchange(value = "${hap.rabbitmq.exchange:hap-appointmentrecords-exchange}", type = "topic"),
            key = "appointment.created"
    ))
    @Counted(value = "saga.appointment.created.consumed", description = "Count of appointment created events consumed")
    public void onAppointmentCreated(AppointmentCreatedEvent event,
                                     Message message,
                                     @Header(name = CORRELATION_ID_HEADER, required = false) String correlationIdHeader) {
        String correlationId = extractCorrelationId(message, correlationIdHeader);
        if (correlationId != null) {
            MDC.put(CORRELATION_ID_HEADER, correlationId);
        }

        log.info("📥 Evento AppointmentCreatedEvent recebido | correlationId={} | appointmentId={} | patientId={} | physicianId={} | dateTime={}",
                correlationId,
                event != null ? event.getAppointmentId() : "null",
                event != null ? event.getPatientId() : "null",
                event != null ? event.getPhysicianId() : "null",
                event != null ? event.getDateTime() : "null");

        try {
            persistEvent("appointment.created", event, correlationId, false);
            sagaCoordinator.recordStep(event != null ? event.getAppointmentId() : null, "APPOINTMENT_CREATED", event, correlationId, false);
        } finally {
            MDC.remove(CORRELATION_ID_HEADER);
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "q.appointmentrecords.projection", durable = "true"),
            exchange = @Exchange(value = "${hap.rabbitmq.exchange:hap-appointmentrecords-exchange}", type = "topic"),
            key = "appointment.updated"
    ))
    @Counted(value = "saga.appointment.updated.consumed", description = "Count of appointment updated events consumed")
    public void onAppointmentUpdated(AppointmentUpdatedEvent event,
                                     Message message,
                                     @Header(name = CORRELATION_ID_HEADER, required = false) String correlationIdHeader) {
        String correlationId = extractCorrelationId(message, correlationIdHeader);
        if (correlationId != null) {
            MDC.put(CORRELATION_ID_HEADER, correlationId);
        }

        log.info("📥 Evento AppointmentUpdatedEvent recebido | correlationId={} | appointmentId={} | status={}→{}",
                correlationId,
                event != null ? event.getAppointmentId() : "null",
                event != null ? event.getPreviousStatus() : "null",
                event != null ? event.getNewStatus() : "null");

        try {
            persistEvent("appointment.updated", event, correlationId, false);
            sagaCoordinator.recordStep(event != null ? event.getAppointmentId() : null, "APPOINTMENT_UPDATED", event, correlationId, false);
        } finally {
            MDC.remove(CORRELATION_ID_HEADER);
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "q.appointmentrecords.projection", durable = "true"),
            exchange = @Exchange(value = "${hap.rabbitmq.exchange:hap-appointmentrecords-exchange}", type = "topic"),
            key = "appointment.canceled"
    ))
    @Counted(value = "saga.appointment.canceled.consumed", description = "Count of appointment canceled events consumed")
    public void onAppointmentCanceled(AppointmentCanceledEvent event,
                                      Message message,
                                      @Header(name = CORRELATION_ID_HEADER, required = false) String correlationIdHeader) {
        String correlationId = extractCorrelationId(message, correlationIdHeader);
        if (correlationId != null) {
            MDC.put(CORRELATION_ID_HEADER, correlationId);
        }

        log.info("📥 Evento AppointmentCanceledEvent recebido | correlationId={} | appointmentId={} | reason={}",
                correlationId,
                event != null ? event.getAppointmentId() : "null",
                event != null ? event.getReason() : "null");

        try {
            persistEvent("appointment.canceled", event, correlationId, true);
            sagaCoordinator.compensate(event != null ? event.getAppointmentId() : null, event != null ? event.getReason() : "canceled", correlationId);
        } finally {
            MDC.remove(CORRELATION_ID_HEADER);
        }
    }

    private void persistEvent(String eventType, Object payloadObj, String correlationId, boolean compensation) {
        try {
            String payload = payloadObj == null ? "{}" : objectMapper.writeValueAsString(payloadObj);
            EventStoreEntry entry = EventStoreEntry.builder()
                    .appointmentId(extractAppointmentId(payloadObj))
                    .eventType(eventType)
                    .payload(payload)
                    .correlationId(correlationId)
                    .compensation(compensation)
                    .occurredAt(Instant.now())
                    .build();
            eventStoreRepository.save(entry);
        } catch (Exception ex) {
            log.warn("Failed to persist event in store | type={} | correlationId={}", eventType, correlationId, ex);
        }
    }

    private String extractAppointmentId(Object payloadObj) {
        if (payloadObj instanceof AppointmentCreatedEvent e) {
            return e.getAppointmentId();
        }
        if (payloadObj instanceof AppointmentUpdatedEvent e) {
            return e.getAppointmentId();
        }
        if (payloadObj instanceof AppointmentCanceledEvent e) {
            return e.getAppointmentId();
        }
        return null;
    }

    private String extractCorrelationId(Message message, String headerValue) {
        if (headerValue != null && !headerValue.isBlank()) {
            return headerValue;
        }
        if (message != null && message.getMessageProperties() != null) {
            Object raw = message.getMessageProperties().getHeaders().get(CORRELATION_ID_HEADER);
            if (raw != null) {
                String value = raw.toString();
                return value.isBlank() ? null : value;
            }
        }
        return null;
    }
}