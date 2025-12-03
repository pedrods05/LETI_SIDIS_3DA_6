package leti_sisdis_6.happhysicians.command;

import leti_sisdis_6.happhysicians.dto.input.ScheduleAppointmentRequest;
import leti_sisdis_6.happhysicians.dto.input.UpdateAppointmentRequest;
import leti_sisdis_6.happhysicians.events.AppointmentCanceledEvent;
import leti_sisdis_6.happhysicians.events.AppointmentCreatedEvent;
import leti_sisdis_6.happhysicians.events.AppointmentUpdatedEvent;
import leti_sisdis_6.happhysicians.model.Appointment;
import leti_sisdis_6.happhysicians.repository.AppointmentRepository;
import leti_sisdis_6.happhysicians.services.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppointmentCommandService {

    private final AppointmentService appointmentService;
    private final AppointmentRepository appointmentRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${hap.rabbitmq.exchange:hap-exchange}")
    private String exchangeName;

    @Transactional
    public Appointment createAppointment(ScheduleAppointmentRequest dto) {
        // Delegate to existing service
        Appointment appointment = appointmentService.createAppointment(dto);
        
        // Publish event
        publishAppointmentCreatedEvent(appointment);
        
        return appointment;
    }

    @Transactional
    public Appointment updateAppointment(String appointmentId, UpdateAppointmentRequest dto) {
        System.out.println("🔍 [Command] Updating appointment: " + appointmentId + " with status: " + (dto.getStatus() != null ? dto.getStatus() : "null"));
        
        // Delegate to existing service
        Appointment appointment = appointmentService.updateAppointment(appointmentId, dto);
        
        if (appointment != null) {
            System.out.println("🔍 [Command] Appointment after update - Status: " + appointment.getStatus());
            
            // Only publish UpdatedEvent if status is NOT CANCELED
            // If status is CANCELED, it means the update actually canceled it, so we should publish CanceledEvent
            if (appointment.getStatus() == leti_sisdis_6.happhysicians.model.AppointmentStatus.CANCELED) {
                System.out.println("⚠️ WARNING: Update resulted in CANCELED status for appointment: " + appointmentId + ". This should not happen during a normal update!");
                publishAppointmentCanceledEvent(appointment);
            } else {
                // Normal update - publish UpdatedEvent
                System.out.println("✅ [Command] Publishing AppointmentUpdatedEvent for appointment: " + appointmentId);
                publishAppointmentUpdatedEvent(appointment);
            }
        }
        
        return appointment;
    }

    @Transactional
    public Appointment cancelAppointment(String appointmentId) {
        // Delegate to existing service
        Appointment appointment = appointmentService.cancelAppointment(appointmentId);
        
        if (appointment != null) {
            // Publish event
            publishAppointmentCanceledEvent(appointment);
        }
        
        return appointment;
    }

    private void publishAppointmentCreatedEvent(Appointment appointment) {
        try {
            AppointmentCreatedEvent event = new AppointmentCreatedEvent(
                    appointment.getAppointmentId(),
                    appointment.getPatientId(),
                    appointment.getPhysician().getPhysicianId(),
                    appointment.getDateTime(),
                    appointment.getConsultationType().toString(),
                    appointment.getStatus().toString()
            );

            rabbitTemplate.convertAndSend(exchangeName, "appointment.created", event);
            System.out.println("⚡ Evento AppointmentCreatedEvent enviado para o RabbitMQ: " + appointment.getAppointmentId());
        } catch (Exception e) {
            System.err.println("⚠️ FALHA ao enviar evento RabbitMQ: " + e.getMessage());
        }
    }

    private void publishAppointmentUpdatedEvent(Appointment appointment) {
        try {
            AppointmentUpdatedEvent event = new AppointmentUpdatedEvent(
                    appointment.getAppointmentId(),
                    appointment.getPatientId(),
                    appointment.getPhysician().getPhysicianId(),
                    appointment.getDateTime(),
                    appointment.getConsultationType().toString(),
                    appointment.getStatus().toString()
            );

            rabbitTemplate.convertAndSend(exchangeName, "appointment.updated", event);
            System.out.println("⚡ Evento AppointmentUpdatedEvent enviado para o RabbitMQ: " + appointment.getAppointmentId());
        } catch (Exception e) {
            System.err.println("⚠️ FALHA ao enviar evento RabbitMQ: " + e.getMessage());
        }
    }

    private void publishAppointmentCanceledEvent(Appointment appointment) {
        try {
            AppointmentCanceledEvent event = new AppointmentCanceledEvent(
                    appointment.getAppointmentId(),
                    appointment.getPatientId(),
                    appointment.getPhysician().getPhysicianId()
            );

            rabbitTemplate.convertAndSend(exchangeName, "appointment.canceled", event);
            System.out.println("⚡ Evento AppointmentCanceledEvent enviado para o RabbitMQ: " + appointment.getAppointmentId());
        } catch (Exception e) {
            System.err.println("⚠️ FALHA ao enviar evento RabbitMQ: " + e.getMessage());
        }
    }
}

