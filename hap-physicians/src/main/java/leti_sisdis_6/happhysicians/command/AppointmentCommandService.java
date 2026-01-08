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
import leti_sisdis_6.happhysicians.services.CompensationService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static leti_sisdis_6.happhysicians.config.RabbitMQConfig.CORRELATION_ID_HEADER;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentCommandService {

    private final AppointmentService appointmentService;
    private final AppointmentRepository appointmentRepository;
    private final RabbitTemplate rabbitTemplate;
    private final EventStoreService eventStoreService;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    @Autowired(required = false)
    private CompensationService compensationService;

    @Value("${hap.rabbitmq.exchange:hap-exchange}")
    private String exchangeName;

    // Circuit breaker and retry configuration for Saga operations
    private static final String SAGA_AMQP_CIRCUIT_BREAKER = "sagaAmqpPublisher";
    private static final String SAGA_AMQP_RETRY = "sagaRetry";

    @Transactional
    public Appointment createAppointment(ScheduleAppointmentRequest dto) {
        return createAppointment(dto, null);
    }

    /**
     * Create appointment - Saga step with resilience patterns.
     * Applies Retry and Circuit Breaker to AMQP message publishing.
     */
    @Transactional
    @TimeLimiter(name = "sagaOperation")
    public CompletableFuture<Appointment> createAppointmentAsync(ScheduleAppointmentRequest dto, String correlationId) {
        Timer.Sample sample = meterRegistry != null ? Timer.start(meterRegistry) : null;
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Delegate to existing service
                Appointment appointment = appointmentService.createAppointment(dto);
                
                // Save to Event Store (Event Sourcing)
                saveEventToStore(appointment, EventType.CONSULTATION_SCHEDULED, appointment, correlationId);
                
                // Publish events with resilience patterns
                publishAppointmentCreatedEventWithResilience(appointment);
                publishAppointmentReminderEventWithResilience(appointment, "CREATED");
                
                return appointment;
            } finally {
                if (sample != null) {
                    sample.stop(meterRegistry.timer("saga.step.duration", 
                        "step", "createAppointment", 
                        "application", "hap-physicians"));
                }
            }
        });
    }

    @Transactional
    public Appointment createAppointment(ScheduleAppointmentRequest dto, String correlationId) {
        Timer.Sample sample = meterRegistry != null ? Timer.start(meterRegistry) : null;
        try {
            // Delegate to existing service
            Appointment appointment = appointmentService.createAppointment(dto);
            
            // Save to Event Store (Event Sourcing)
            saveEventToStore(appointment, EventType.CONSULTATION_SCHEDULED, appointment, correlationId);
            
            // Publish events with resilience patterns
            publishAppointmentCreatedEventWithResilience(appointment);
            publishAppointmentReminderEventWithResilience(appointment, "CREATED");
            
            return appointment;
        } finally {
            if (sample != null) {
                sample.stop(meterRegistry.timer("saga.step.duration", 
                    "step", "createAppointment", 
                    "application", "hap-physicians"));
            }
        }
    }

    @Transactional
    public Appointment updateAppointment(String appointmentId, UpdateAppointmentRequest dto) {
        return updateAppointment(appointmentId, dto, null);
    }

    @Transactional
    public Appointment updateAppointment(String appointmentId, UpdateAppointmentRequest dto, String correlationId) {
        log.debug("🔍 [Command] Updating appointment: {} with status: {}", appointmentId, dto.getStatus() != null ? dto.getStatus() : "null");
        
        // Get previous state for comparison
        Appointment previousAppointment = appointmentRepository.findById(appointmentId).orElse(null);
        
        // Delegate to existing service
        Appointment appointment = appointmentService.updateAppointment(appointmentId, dto);
        
        if (appointment != null) {
            log.debug("🔍 [Command] Appointment after update - Status: {}", appointment.getStatus());
            
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
            saveEventToStore(appointment, eventType, appointment, correlationId);
            
            // Only publish UpdatedEvent if status is NOT CANCELED
            // If status is CANCELED, it means the update actually canceled it, so we should publish CanceledEvent
            if (appointment.getStatus() == AppointmentStatus.CANCELED) {
                log.warn("⚠️ WARNING: Update resulted in CANCELED status for appointment: {}. This should not happen during a normal update!", appointmentId);
                publishAppointmentCanceledEvent(appointment);
            } else {
                // Normal update - publish UpdatedEvent
                log.info("✅ [Command] Publishing AppointmentUpdatedEvent for appointment: {}", appointmentId);
                publishAppointmentUpdatedEvent(appointment);
                publishAppointmentReminderEvent(appointment, "UPDATED");
            }
        }
        
        return appointment;
    }

    @Transactional
    public Appointment cancelAppointment(String appointmentId) {
        return cancelAppointment(appointmentId, null);
    }

    @Transactional
    public Appointment cancelAppointment(String appointmentId, String correlationId) {
        Timer.Sample sample = meterRegistry != null ? Timer.start(meterRegistry) : null;
        try {
            // Delegate to existing service
            Appointment appointment = appointmentService.cancelAppointment(appointmentId);
            
            if (appointment != null) {
                // Save to Event Store (Event Sourcing)
                saveEventToStore(appointment, EventType.CONSULTATION_CANCELED, appointment, correlationId);
                
                // Publish event with resilience patterns
                publishAppointmentCanceledEventWithResilience(appointment);
                
                // If compensation is needed (e.g., rollback operations), use Bulkhead isolation
                if (compensationService != null) {
                    // Example: Execute compensation logic with Bulkhead isolation
                    // This ensures compensation operations don't consume all resources
                    log.debug("🔄 [Saga] Compensation service available for potential rollback operations");
                }
            }
            
            return appointment;
        } finally {
            if (sample != null) {
                sample.stop(meterRegistry.timer("saga.step.duration", 
                    "step", "cancelAppointment", 
                    "application", "hap-physicians"));
            }
        }
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
        
        // Try to get correlation ID from MDC if available
        String correlationId = null;
        try {
            correlationId = org.slf4j.MDC.get("X-Correlation-Id");
        } catch (Exception e) {
            // Ignore if MDC is not available
        }
        
        eventStoreService.saveEvent(
                appointmentId,
                EventType.NOTE_ADDED,
                noteData,
                correlationId, // correlationId from MDC if available
                userId,
                metadata
        );
        
        log.info("📝 [Event Store] NoteAdded event saved for appointment: {}", appointmentId);
    }
    
    /**
     * Helper method to save events to Event Store (Event Sourcing)
     */
    private void saveEventToStore(Appointment appointment, EventType eventType, Object eventData) {
        saveEventToStore(appointment, eventType, eventData, null);
    }

    /**
     * Helper method to save events to Event Store (Event Sourcing) with correlation ID
     */
    private void saveEventToStore(Appointment appointment, EventType eventType, Object eventData, String correlationId) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", "hap-physicians");
            metadata.put("operation", eventType.getValue());
            
            eventStoreService.saveEvent(
                    appointment.getAppointmentId(),
                    eventType,
                    eventData,
                    correlationId, // correlationId from HTTP header
                    null, // userId (pode ser adicionado depois)
                    metadata
            );
            log.info("📝 [Event Store] Event saved: {} for appointment: {} | correlationId: {}", eventType.getValue(), appointment.getAppointmentId(), correlationId);
        } catch (Exception e) {
            log.error("⚠️ [Event Store] Failed to save event: {}", e.getMessage(), e);
            // Não lança exceção para não quebrar o fluxo principal
        }
    }

    /**
     * Publish appointment created event with resilience patterns.
     * Applies Circuit Breaker and Retry for AMQP message publishing in Saga flows.
     */
    @CircuitBreaker(name = SAGA_AMQP_CIRCUIT_BREAKER, fallbackMethod = "publishAppointmentCreatedEventFallback")
    @Retry(name = SAGA_AMQP_RETRY)
    private void publishAppointmentCreatedEventWithResilience(Appointment appointment) {
        AppointmentCreatedEvent event = new AppointmentCreatedEvent(
                appointment.getAppointmentId(),
                appointment.getPatientId(),
                appointment.getPhysician().getPhysicianId(),
                appointment.getDateTime(),
                appointment.getConsultationType().toString(),
                appointment.getStatus().toString()
        );

        rabbitTemplate.convertAndSend(exchangeName, "appointment.created", event);
        log.info("⚡ Evento AppointmentCreatedEvent enviado para o RabbitMQ: {}", appointment.getAppointmentId());
        
        // Track AMQP message published
        if (meterRegistry != null) {
            Counter.builder("amqp.messages.published")
                .tag("event.type", "appointment.created")
                .tag("application", "hap-physicians")
                .register(meterRegistry)
                .increment();
        }
    }

    /**
     * Fallback method for Circuit Breaker when AMQP publishing fails.
     */
    private void publishAppointmentCreatedEventFallback(Appointment appointment, Exception e) {
        log.error("⚠️ [Circuit Breaker] Failed to publish AppointmentCreatedEvent, circuit breaker opened: {}", e.getMessage());
        if (meterRegistry != null) {
            Counter.builder("amqp.messages.failed")
                .tag("event.type", "appointment.created")
                .tag("application", "hap-physicians")
                .tag("failure.reason", "circuit_breaker_open")
                .register(meterRegistry)
                .increment();
        }
        // In a real scenario, you might want to store this in a dead letter queue
        // or retry later via a scheduled job
    }

    private void publishAppointmentCreatedEvent(Appointment appointment) {
        publishAppointmentCreatedEventWithResilience(appointment);
    }

    /**
     * Publish appointment updated event with resilience patterns.
     * Applies Circuit Breaker and Retry for AMQP message publishing in Saga flows.
     */
    @CircuitBreaker(name = SAGA_AMQP_CIRCUIT_BREAKER, fallbackMethod = "publishAppointmentUpdatedEventFallback")
    @Retry(name = SAGA_AMQP_RETRY)
    private void publishAppointmentUpdatedEventWithResilience(Appointment appointment) {
        AppointmentUpdatedEvent event = new AppointmentUpdatedEvent(
                appointment.getAppointmentId(),
                appointment.getPatientId(),
                appointment.getPhysician().getPhysicianId(),
                appointment.getDateTime(),
                appointment.getConsultationType().toString(),
                appointment.getStatus().toString()
        );

        rabbitTemplate.convertAndSend(exchangeName, "appointment.updated", event);
        log.info("⚡ Evento AppointmentUpdatedEvent enviado para o RabbitMQ: {}", appointment.getAppointmentId());
        
        // Track AMQP message published
        if (meterRegistry != null) {
            Counter.builder("amqp.messages.published")
                .tag("event.type", "appointment.updated")
                .tag("application", "hap-physicians")
                .register(meterRegistry)
                .increment();
        }
    }

    /**
     * Fallback method for Circuit Breaker when AMQP publishing fails.
     */
    private void publishAppointmentUpdatedEventFallback(Appointment appointment, Exception e) {
        log.error("⚠️ [Circuit Breaker] Failed to publish AppointmentUpdatedEvent, circuit breaker opened: {}", e.getMessage());
        if (meterRegistry != null) {
            Counter.builder("amqp.messages.failed")
                .tag("event.type", "appointment.updated")
                .tag("application", "hap-physicians")
                .tag("failure.reason", "circuit_breaker_open")
                .register(meterRegistry)
                .increment();
        }
    }

    private void publishAppointmentUpdatedEvent(Appointment appointment) {
        publishAppointmentUpdatedEventWithResilience(appointment);
    }

    /**
     * Publish appointment canceled event with resilience patterns.
     * Applies Circuit Breaker and Retry for AMQP message publishing in Saga flows.
     */
    @CircuitBreaker(name = SAGA_AMQP_CIRCUIT_BREAKER, fallbackMethod = "publishAppointmentCanceledEventFallback")
    @Retry(name = SAGA_AMQP_RETRY)
    private void publishAppointmentCanceledEventWithResilience(Appointment appointment) {
        AppointmentCanceledEvent event = new AppointmentCanceledEvent(
                appointment.getAppointmentId(),
                appointment.getPatientId(),
                appointment.getPhysician().getPhysicianId()
        );

        rabbitTemplate.convertAndSend(exchangeName, "appointment.canceled", event);
        log.info("⚡ Evento AppointmentCanceledEvent enviado para o RabbitMQ: {}", appointment.getAppointmentId());
        
        // Track AMQP message published
        if (meterRegistry != null) {
            Counter.builder("amqp.messages.published")
                .tag("event.type", "appointment.canceled")
                .tag("application", "hap-physicians")
                .register(meterRegistry)
                .increment();
        }
    }

    /**
     * Fallback method for Circuit Breaker when AMQP publishing fails.
     */
    private void publishAppointmentCanceledEventFallback(Appointment appointment, Exception e) {
        log.error("⚠️ [Circuit Breaker] Failed to publish AppointmentCanceledEvent, circuit breaker opened: {}", e.getMessage());
        if (meterRegistry != null) {
            Counter.builder("amqp.messages.failed")
                .tag("event.type", "appointment.canceled")
                .tag("application", "hap-physicians")
                .tag("failure.reason", "circuit_breaker_open")
                .register(meterRegistry)
                .increment();
        }
    }

    private void publishAppointmentCanceledEvent(Appointment appointment) {
        publishAppointmentCanceledEventWithResilience(appointment);
    }

    /**
     * Publish appointment reminder event with resilience patterns.
     * Applies Circuit Breaker and Retry for AMQP message publishing in Saga flows.
     */
    @CircuitBreaker(name = SAGA_AMQP_CIRCUIT_BREAKER, fallbackMethod = "publishAppointmentReminderEventFallback")
    @Retry(name = SAGA_AMQP_RETRY)
    private void publishAppointmentReminderEventWithResilience(Appointment appointment, String reminderType) {
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
        log.info("⚡ Evento AppointmentReminderEvent enviado para o RabbitMQ: {} (tipo: {})", appointment.getAppointmentId(), reminderType);
        
        // Track AMQP message published
        if (meterRegistry != null) {
            Counter.builder("amqp.messages.published")
                .tag("event.type", "appointment.reminder")
                .tag("application", "hap-physicians")
                .register(meterRegistry)
                .increment();
        }
    }

    /**
     * Fallback method for Circuit Breaker when AMQP publishing fails.
     */
    private void publishAppointmentReminderEventFallback(Appointment appointment, String reminderType, Exception e) {
        log.error("⚠️ [Circuit Breaker] Failed to publish AppointmentReminderEvent, circuit breaker opened: {}", e.getMessage());
        if (meterRegistry != null) {
            Counter.builder("amqp.messages.failed")
                .tag("event.type", "appointment.reminder")
                .tag("application", "hap-physicians")
                .tag("failure.reason", "circuit_breaker_open")
                .register(meterRegistry)
                .increment();
        }
    }

    private void publishAppointmentReminderEvent(Appointment appointment, String reminderType) {
        publishAppointmentReminderEventWithResilience(appointment, reminderType);
    }
}

