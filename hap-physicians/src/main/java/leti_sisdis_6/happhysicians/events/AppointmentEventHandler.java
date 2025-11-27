package leti_sisdis_6.happhysicians.events;

import leti_sisdis_6.happhysicians.query.AppointmentQueryRepository;
import leti_sisdis_6.happhysicians.query.AppointmentSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppointmentEventHandler {

    private final AppointmentQueryRepository queryRepository;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "q.appointment.summary.updater", durable = "true"),
            exchange = @Exchange(value = "${hap.rabbitmq.exchange:hap-exchange}", type = "topic"),
            key = "appointment.created"
    ))
    public void handleAppointmentCreated(AppointmentCreatedEvent event) {
        System.out.println("📥 [Query Side] Recebi evento AppointmentCreated: " + event.getAppointmentId());

        AppointmentSummary summary = new AppointmentSummary(
                event.getAppointmentId(),
                event.getPatientId(),
                event.getPhysicianId(),
                event.getDateTime(),
                event.getConsultationType(),
                event.getStatus()
        );

        queryRepository.save(summary);

        System.out.println("✅ [Query Side] Appointment guardado no MongoDB: " + summary.getId());
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "q.appointment.summary.updater", durable = "true"),
            exchange = @Exchange(value = "${hap.rabbitmq.exchange:hap-exchange}", type = "topic"),
            key = "appointment.updated"
    ))
    public void handleAppointmentUpdated(AppointmentUpdatedEvent event) {
        System.out.println("📥 [Query Side] Recebi evento AppointmentUpdated: " + event.getAppointmentId());

        AppointmentSummary summary = new AppointmentSummary(
                event.getAppointmentId(),
                event.getPatientId(),
                event.getPhysicianId(),
                event.getDateTime(),
                event.getConsultationType(),
                event.getStatus()
        );

        queryRepository.save(summary);

        System.out.println("✅ [Query Side] Appointment atualizado no MongoDB: " + summary.getId());
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "q.appointment.summary.updater", durable = "true"),
            exchange = @Exchange(value = "${hap.rabbitmq.exchange:hap-exchange}", type = "topic"),
            key = "appointment.canceled"
    ))
    public void handleAppointmentCanceled(AppointmentCanceledEvent event) {
        System.out.println("📥 [Query Side] Recebi evento AppointmentCanceled: " + event.getAppointmentId());

        // Update status to CANCELED
        queryRepository.findById(event.getAppointmentId()).ifPresent(summary -> {
            summary.setStatus("CANCELED");
            queryRepository.save(summary);
            System.out.println("✅ [Query Side] Appointment cancelado no MongoDB: " + summary.getId());
        });
    }
}

