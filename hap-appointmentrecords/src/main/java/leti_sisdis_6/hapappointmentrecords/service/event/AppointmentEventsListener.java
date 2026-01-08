package leti_sisdis_6.hapappointmentrecords.service.event;

import io.micrometer.core.annotation.Counted;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import static leti_sisdis_6.hapappointmentrecords.config.RabbitMQConfig.CORRELATION_ID_HEADER;

/**
 * Listens to appointment events from physicians service for logging/monitoring purposes.
 * AppointmentRecords does NOT store appointment data locally - it queries physicians service when needed.
 */
@Component
@Slf4j
public class AppointmentEventsListener {

    public AppointmentEventsListener() {
    }

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
        } finally {
            MDC.remove(CORRELATION_ID_HEADER);
        }
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