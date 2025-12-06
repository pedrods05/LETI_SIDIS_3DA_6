package leti_sisdis_6.hapappointmentrecords.service.event;

import leti_sisdis_6.hapappointmentrecords.model.Appointment;
import leti_sisdis_6.hapappointmentrecords.model.AppointmentProjection;
import leti_sisdis_6.hapappointmentrecords.repository.AppointmentProjectionRepository;
import leti_sisdis_6.hapappointmentrecords.repository.AppointmentRepository;
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

@Component
@Slf4j
public class AppointmentEventsListener {

    private final AppointmentProjectionRepository projectionRepository;
    private final AppointmentRepository appointmentRepository;

    public AppointmentEventsListener(AppointmentProjectionRepository projectionRepository, AppointmentRepository appointmentRepository) {
        this.projectionRepository = projectionRepository;
        this.appointmentRepository = appointmentRepository;
    }

    // Backward-compatible overload used by existing tests
    public void onAppointmentCreated(AppointmentCreatedEvent event) {
        onAppointmentCreated(event, null, null);
    }

    // Método público que processa eventos; pode ser anotado com @RabbitListener em runtime/config se necessário
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "q.appointmentrecords.projection", durable = "true"),
            exchange = @Exchange(value = "${hap.rabbitmq.exchange:hap-appointmentrecords-exchange}", type = "topic"),
            key = "appointment.created"
    ))
    public void onAppointmentCreated(AppointmentCreatedEvent event,
                                     Message message,
                                     @Header(name = CORRELATION_ID_HEADER, required = false) String correlationIdHeader) {
        String correlationId = extractCorrelationId(message, correlationIdHeader);
        if (correlationId != null) {
            MDC.put(CORRELATION_ID_HEADER, correlationId);
        }

        log.info("📥 Evento AppointmentCreatedEvent recebido | correlationId={} | appointmentId={}",
                correlationId, event != null ? event.getAppointmentId() : "null");

        try {
            if (event == null || event.getAppointmentId() == null) return;

            AppointmentProjection projection = AppointmentProjection.builder()
                    .appointmentId(event.getAppointmentId())
                    .patientId(event.getPatientId())
                    .physicianId(event.getPhysicianId())
                    .dateTime(event.getDateTime())
                    .consultationType(event.getConsultationType())
                    .status(event.getStatus())
                    .lastUpdated(event.getOccurredAt())
                    .build();

            projectionRepository.save(projection);

            // Optionally keep a local source-of-truth in the write model as well
            Appointment appointment = Appointment.builder()
                    .appointmentId(event.getAppointmentId())
                    .patientId(event.getPatientId())
                    .physicianId(event.getPhysicianId())
                    .dateTime(event.getDateTime())
                    .consultationType(event.getConsultationType())
                    .status(event.getStatus())
                    .build();

            appointmentRepository.save(appointment);
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
