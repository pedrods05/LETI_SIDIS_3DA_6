package leti_sisdis_6.happhysicians.command;

import leti_sisdis_6.happhysicians.dto.input.ScheduleAppointmentRequest;
import leti_sisdis_6.happhysicians.dto.input.UpdateAppointmentRequest;
import leti_sisdis_6.happhysicians.events.AppointmentCanceledEvent;
import leti_sisdis_6.happhysicians.events.AppointmentCreatedEvent;
import leti_sisdis_6.happhysicians.events.AppointmentReminderEvent;
import leti_sisdis_6.happhysicians.events.AppointmentUpdatedEvent;
import leti_sisdis_6.happhysicians.eventsourcing.EventStoreService;
import leti_sisdis_6.happhysicians.eventsourcing.EventType;
import leti_sisdis_6.happhysicians.model.Appointment;
import leti_sisdis_6.happhysicians.model.AppointmentStatus;
import leti_sisdis_6.happhysicians.repository.AppointmentRepository;
import leti_sisdis_6.happhysicians.services.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AppointmentCommandService {

    private final AppointmentService appointmentService;
    private final AppointmentRepository appointmentRepository;
    private final RabbitTemplate rabbitTemplate;
    private final EventStoreService eventStoreService;

    @Value("${hap.rabbitmq.exchange:hap-exchange}")
    private String exchangeName;

    @Transactional
    public Appointment createAppointment(ScheduleAppointmentRequest dto) {
        // Delegate to existing service
        Appointment appointment = appointmentService.createAppointment(dto);
        
        // Save to Event Store (Event Sourcing)
        saveEventToStore(appointment, EventType.CONSULTATION_SCHEDULED, appointment);
        
        // Publish events
        publishAppointmentCreatedEvent(appointment);
        publishAppointmentReminderEvent(appointment, "CREATED");
        
        return appointment;
    }

    @Transactional
    public Appointment updateAppointment(String appointmentId, UpdateAppointmentRequest dto) {
        System.out.println("🔍 [Command] Updating appointment: " + appointmentId + " with status: " + (dto.getStatus() != null ? dto.getStatus() : "null"));
        
        // Get previous state for comparison
        Appointment previousAppointment = appointmentRepository.findById(appointmentId).orElse(null);
        
        // Delegate to existing service
        Appointment appointment = appointmentService.updateAppointment(appointmentId, dto);
        
        if (appointment != null) {
            System.out.println("🔍 [Command] Appointment after update - Status: " + appointment.getStatus());
            
            // Determine event type based on what changed
            EventType eventType;
            if (appointment.getStatus() == AppointmentStatus.CANCELED) {
                eventType = EventType.CONSULTATION_CANCELED;
            } else if (appointment.getStatus() == AppointmentStatus.COMPLETED && 
                       previousAppointment != null && 
                       previousAppointment.getStatus() != AppointmentStatus.COMPLETED) {
                // Status changed to COMPLETED
                eventType = EventType.CONSULTATION_COMPLETED;
            } else if (appointment.isWasRescheduled() && previousAppointment != null && 
                       !appointment.getDateTime().equals(previousAppointment.getDateTime())) {
                eventType = EventType.CONSULTATION_RESCHEDULED;
            } else {
                eventType = EventType.CONSULTATION_UPDATED;
            }
            
            // Save to Event Store (Event Sourcing)
            saveEventToStore(appointment, eventType, appointment);
            
            // Only publish UpdatedEvent if status is NOT CANCELED
            // If status is CANCELED, it means the update actually canceled it, so we should publish CanceledEvent
            if (appointment.getStatus() == AppointmentStatus.CANCELED) {
                System.out.println("⚠️ WARNING: Update resulted in CANCELED status for appointment: " + appointmentId + ". This should not happen during a normal update!");
                publishAppointmentCanceledEvent(appointment);
            } else {
                // Normal update - publish UpdatedEvent
                System.out.println("✅ [Command] Publishing AppointmentUpdatedEvent for appointment: " + appointmentId);
                publishAppointmentUpdatedEvent(appointment);
                publishAppointmentReminderEvent(appointment, "UPDATED");
            }
        }
        
        return appointment;
    }

    @Transactional
    public Appointment cancelAppointment(String appointmentId) {
        // Delegate to existing service
        Appointment appointment = appointmentService.cancelAppointment(appointmentId);
        
        if (appointment != null) {
            // Save to Event Store (Event Sourcing)
            saveEventToStore(appointment, EventType.CONSULTATION_CANCELED, appointment);
            
            // Publish event
            publishAppointmentCanceledEvent(appointment);
        }
        
        return appointment;
    }
    
    /**
     * Adiciona uma nota a uma consulta (gera evento NoteAdded)
     * 
     * @param appointmentId ID da consulta
     * @param note Conteúdo da nota
     * @param userId ID do usuário que adicionou a nota (opcional)
     */
    @Transactional
    public void addNoteToAppointment(String appointmentId, String note, String userId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found: " + appointmentId));
        
        // Criar objeto com a nota
        Map<String, Object> noteData = new HashMap<>();
        noteData.put("appointmentId", appointmentId);
        noteData.put("note", note);
        noteData.put("addedAt", LocalDateTime.now().toString());
        noteData.put("appointment", appointment);
        
        // Salvar evento NoteAdded no Event Store
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "hap-physicians");
        metadata.put("operation", "NoteAdded");
        
        eventStoreService.saveEvent(
                appointmentId,
                EventType.NOTE_ADDED,
                noteData,
                null, // correlationId
                userId,
                metadata
        );
        
        System.out.println("📝 [Event Store] NoteAdded event saved for appointment: " + appointmentId);
    }
    
    /**
     * Helper method to save events to Event Store (Event Sourcing)
     */
    private void saveEventToStore(Appointment appointment, EventType eventType, Object eventData) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", "hap-physicians");
            metadata.put("operation", eventType.getValue());
            
            eventStoreService.saveEvent(
                    appointment.getAppointmentId(),
                    eventType,
                    eventData,
                    null, // correlationId (pode ser adicionado depois)
                    null, // userId (pode ser adicionado depois)
                    metadata
            );
            System.out.println("📝 [Event Store] Event saved: " + eventType.getValue() + " for appointment: " + appointment.getAppointmentId());
        } catch (Exception e) {
            System.err.println("⚠️ [Event Store] Failed to save event: " + e.getMessage());
            // Não lança exceção para não quebrar o fluxo principal
        }
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

    private void publishAppointmentReminderEvent(Appointment appointment, String reminderType) {
        try {
            AppointmentReminderEvent event = new AppointmentReminderEvent(
                    appointment.getAppointmentId(),
                    appointment.getPatientId(),
                    appointment.getPatientName(),
                    appointment.getPatientEmail(),
                    appointment.getPatientPhone(),
                    appointment.getPhysician().getPhysicianId(),
                    appointment.getPhysician().getFullName(),
                    appointment.getDateTime(),
                    appointment.getConsultationType().toString(),
                    reminderType
            );

            rabbitTemplate.convertAndSend(exchangeName, "appointment.reminder", event);
            System.out.println("⚡ Evento AppointmentReminderEvent enviado para o RabbitMQ: " + appointment.getAppointmentId() + " (tipo: " + reminderType + ")");
        } catch (Exception e) {
            System.err.println("⚠️ FALHA ao enviar evento AppointmentReminderEvent: " + e.getMessage());
        }
    }
}

