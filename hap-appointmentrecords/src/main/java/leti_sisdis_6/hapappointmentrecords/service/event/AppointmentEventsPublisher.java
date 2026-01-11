package leti_sisdis_6.hapappointmentrecords.service.event;

import static leti_sisdis_6.hapappointmentrecords.config.RabbitMQConfig.CORRELATION_ID_HEADER;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentEventsPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${hap.rabbitmq.exchange:hap-appointmentrecords-exchange}")
    private String exchange;

    @Value("${hap.events.records.enabled:false}")
    private boolean recordEventsEnabled;

    public void publishAppointmentCreated(AppointmentCreatedEvent event) {
        String correlationId = resolveCorrelationId();
        try {
            byte[] payload = objectMapper.writeValueAsBytes(event);
            MessageProperties props = new MessageProperties();
            props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            props.setHeader(CORRELATION_ID_HEADER, correlationId);

            Message message = new Message(payload, props);
            rabbitTemplate.send(exchange, "appointment.created", message);

            log.info("⚡ Evento AppointmentCreatedEvent enviado | correlationId={} | appointmentId={}",
                    correlationId, event != null ? event.getAppointmentId() : "null");
        } catch (Exception ex) {
            log.error("Failed to publish AppointmentCreatedEvent | correlationId={}", correlationId, ex);
        } finally {
            cleanupCorrelationId(correlationId);
        }
    }

    public void publishAppointmentRecordCreated(AppointmentRecordCreatedEvent event) {
        if (!recordEventsEnabled) {
            log.debug("Record events disabled; skipping publish for recordId={}", event != null ? event.getRecordId() : "null");
            return;
        }
        String correlationId = resolveCorrelationId();
        try {
            byte[] payload = objectMapper.writeValueAsBytes(event);
            MessageProperties props = new MessageProperties();
            props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            props.setHeader(CORRELATION_ID_HEADER, correlationId);

            Message message = new Message(payload, props);
            rabbitTemplate.send(exchange, "appointmentrecord.created", message);

            log.info("⚡ Evento AppointmentRecordCreatedEvent enviado | correlationId={} | recordId={} | appointmentId={}",
                    correlationId,
                    event != null ? event.getRecordId() : "null",
                    event != null ? event.getAppointmentId() : "null");
        } catch (Exception ex) {
            log.error("Failed to publish AppointmentRecordCreatedEvent | correlationId={}", correlationId, ex);
        } finally {
            cleanupCorrelationId(correlationId);
        }
    }

    public void publishAppointmentUpdated(AppointmentUpdatedEvent event) {
        String correlationId = resolveCorrelationId();
        try {
            byte[] payload = objectMapper.writeValueAsBytes(event);
            MessageProperties props = new MessageProperties();
            props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            props.setHeader(CORRELATION_ID_HEADER, correlationId);

            Message message = new Message(payload, props);
            rabbitTemplate.send(exchange, "appointment.updated", message);

            log.info("⚡ Evento AppointmentUpdatedEvent enviado | correlationId={} | appointmentId={} | status={}→{}",
                    correlationId,
                    event != null ? event.getAppointmentId() : "null",
                    event != null ? event.getPreviousStatus() : "null",
                    event != null ? event.getNewStatus() : "null");
        } catch (Exception ex) {
            log.error("Failed to publish AppointmentUpdatedEvent | correlationId={}", correlationId, ex);
        } finally {
            cleanupCorrelationId(correlationId);
        }
    }

    public void publishAppointmentCanceled(AppointmentCanceledEvent event) {
        String correlationId = resolveCorrelationId();
        try {
            byte[] payload = objectMapper.writeValueAsBytes(event);
            MessageProperties props = new MessageProperties();
            props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            props.setHeader(CORRELATION_ID_HEADER, correlationId);

            Message message = new Message(payload, props);
            rabbitTemplate.send(exchange, "appointment.canceled", message);

            log.info("⚡ Evento AppointmentCanceledEvent enviado | correlationId={} | appointmentId={} | reason={}",
                    correlationId,
                    event != null ? event.getAppointmentId() : "null",
                    event != null ? event.getReason() : "null");
        } catch (Exception ex) {
            log.error("Failed to publish AppointmentCanceledEvent | correlationId={}", correlationId, ex);
        } finally {
            cleanupCorrelationId(correlationId);
        }
    }

    private String resolveCorrelationId() {
        String correlationId = MDC.get(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
            MDC.put(CORRELATION_ID_HEADER, correlationId);
        }
        return correlationId;
    }

    private void cleanupCorrelationId(String correlationId) {
        String current = MDC.get(CORRELATION_ID_HEADER);
        if (correlationId != null && correlationId.equals(current)) {
            MDC.remove(CORRELATION_ID_HEADER);
        }
    }
}
