package leti_sisdis_6.hapappointmentrecords.service.event;

import leti_sisdis_6.hapappointmentrecords.model.Appointment;
import leti_sisdis_6.hapappointmentrecords.model.AppointmentProjection;
import leti_sisdis_6.hapappointmentrecords.repository.AppointmentProjectionRepository;
import leti_sisdis_6.hapappointmentrecords.repository.AppointmentRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AppointmentEventsListener {

    private final AppointmentProjectionRepository projectionRepository;
    private final AppointmentRepository appointmentRepository;

    public AppointmentEventsListener(AppointmentProjectionRepository projectionRepository, AppointmentRepository appointmentRepository) {
        this.projectionRepository = projectionRepository;
        this.appointmentRepository = appointmentRepository;
    }

    // Método público que processa eventos; anotado com @RabbitListener para consumo direto
    @RabbitListener(queues = "${hap.rabbitmq.queue:appointment.record.created}")
    public void onAppointmentCreated(AppointmentCreatedEvent event) {
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
    }
}
