package leti_sisdis_6.happhysicians.events;

import leti_sisdis_6.happhysicians.model.Appointment;
import leti_sisdis_6.happhysicians.query.AppointmentQueryRepository;
import leti_sisdis_6.happhysicians.query.AppointmentSummary;
import leti_sisdis_6.happhysicians.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentEventHandler {

    private final AppointmentQueryRepository queryRepository;
    private final AppointmentRepository appointmentRepository;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "q.appointment.summary.updater.${spring.profiles.active}", durable = "true"),
            exchange = @Exchange(value = "${hap.rabbitmq.exchange:hap-exchange}", type = "topic"),
            key = "appointment.created"
    ))
    public void handleAppointmentCreated(AppointmentCreatedEvent event) {
        log.info("📥 [Query Side] Recebi evento AppointmentCreated: {}", event.getAppointmentId());

        // Verificar se o summary já existe
        Optional<AppointmentSummary> existingSummary = queryRepository.findById(event.getAppointmentId());
        
        // Se já existe, verificar o status atual no write model para evitar sobrescrever com eventos antigos
        if (existingSummary.isPresent()) {
            AppointmentSummary summary = existingSummary.get();
            Optional<Appointment> appointmentOpt = appointmentRepository.findById(event.getAppointmentId());
            
            if (appointmentOpt.isPresent()) {
                Appointment appointment = appointmentOpt.get();
                // Se o appointment no write model está cancelado, não vamos sobrescrever com um evento de criação antigo
                if (appointment.getStatus() == leti_sisdis_6.happhysicians.model.AppointmentStatus.CANCELED) {
                    log.warn("⚠️ [Query Side] Ignorando evento AppointmentCreated - Appointment está CANCELED no write model. Appointment ID: {}", event.getAppointmentId());
                    return;
                }
                // Se o status no write model é diferente do evento, usar o status do write model (mais atualizado)
                if (!appointment.getStatus().toString().equals(event.getStatus())) {
                    log.warn("⚠️ [Query Side] Status no write model ({}) diferente do evento ({}). Usando status do write model.", appointment.getStatus(), event.getStatus());
                    // Atualizar summary com dados do write model
                    summary.setId(appointment.getAppointmentId());
                    summary.setPatientId(appointment.getPatientId());
                    summary.setPhysicianId(appointment.getPhysician().getPhysicianId());
                    summary.setDateTime(appointment.getDateTime());
                    summary.setConsultationType(appointment.getConsultationType().toString());
                    summary.setStatus(appointment.getStatus().toString());
                    queryRepository.save(summary);
                    log.info("✅ [Query Side] Appointment atualizado no MongoDB com status do write model: {}", summary.getId());
                    return;
                }
            }
        }

        // Criar novo summary ou atualizar existente
        AppointmentSummary summary = existingSummary.orElse(new AppointmentSummary());
        summary.setId(event.getAppointmentId());
        summary.setPatientId(event.getPatientId());
        summary.setPhysicianId(event.getPhysicianId());
        summary.setDateTime(event.getDateTime());
        summary.setConsultationType(event.getConsultationType());
        summary.setStatus(event.getStatus());

        queryRepository.save(summary);

        log.info("✅ [Query Side] Appointment guardado no MongoDB: {}", summary.getId());
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "q.appointment.summary.updater.${spring.profiles.active}", durable = "true"),
            exchange = @Exchange(value = "${hap.rabbitmq.exchange:hap-exchange}", type = "topic"),
            key = "appointment.updated"
    ))
    public void handleAppointmentUpdated(AppointmentUpdatedEvent event) {
        log.info("📥 [Query Side] Recebi evento AppointmentUpdated: {} (Status: {})", event.getAppointmentId(), event.getStatus());

        // Buscar summary existente ou criar novo
        AppointmentSummary summary = queryRepository.findById(event.getAppointmentId())
                .orElse(new AppointmentSummary());

        // Atualizar todos os campos
        summary.setId(event.getAppointmentId());
        summary.setPatientId(event.getPatientId());
        summary.setPhysicianId(event.getPhysicianId());
        summary.setDateTime(event.getDateTime());
        summary.setConsultationType(event.getConsultationType());
        summary.setStatus(event.getStatus()); // ✅ Atualiza o status (incluindo COMPLETED)

        queryRepository.save(summary);

        log.info("✅ [Query Side] Appointment atualizado no MongoDB: {} (Status: {})", summary.getId(), summary.getStatus());
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "q.appointment.summary.updater.${spring.profiles.active}", durable = "true"),
            exchange = @Exchange(value = "${hap.rabbitmq.exchange:hap-exchange}", type = "topic"),
            key = "appointment.canceled"
    ))
    public void handleAppointmentCanceled(AppointmentCanceledEvent event) {
        log.info("📥 [Query Side] Recebi evento AppointmentCanceled: {} (PatientId: {}, PhysicianId: {})", event.getAppointmentId(), event.getPatientId(), event.getPhysicianId());

        // Buscar summary existente ou criar novo
        AppointmentSummary summary = queryRepository.findById(event.getAppointmentId())
                .orElse(new AppointmentSummary());

        // Validação: Se o summary já existe e tem um status ativo (SCHEDULED ou COMPLETED),
        // pode ser um evento antigo. Vamos ignorar para evitar sobrescrever updates mais recentes.
        // Nota: Esta é uma validação simples. Em produção, seria melhor usar timestamps ou versionamento.
        if (summary.getId() != null && summary.getStatus() != null) {
            String currentStatus = summary.getStatus();
            if ("SCHEDULED".equals(currentStatus) || "COMPLETED".equals(currentStatus)) {
                log.warn("⚠️ [Query Side] Ignorando evento de cancelamento - Summary tem status {} (pode ser um evento antigo). Appointment ID: {}", currentStatus, event.getAppointmentId());
                return; // Ignorar evento antigo
            }
        }

        // Buscar appointment completo do write model para preservar todos os campos
        Optional<Appointment> appointmentOpt = appointmentRepository.findById(event.getAppointmentId());
        
        if (appointmentOpt.isPresent()) {
            Appointment appointment = appointmentOpt.get();
            // Preservar todos os campos do appointment original
            summary.setId(appointment.getAppointmentId());
            summary.setPatientId(appointment.getPatientId());
            summary.setPhysicianId(appointment.getPhysician().getPhysicianId());
            summary.setDateTime(appointment.getDateTime());
            summary.setConsultationType(appointment.getConsultationType().toString());
            summary.setStatus("CANCELED"); // Apenas atualizar o status
        } else {
            // Se não encontrar no write model, usar dados do evento (fallback)
            summary.setId(event.getAppointmentId());
            summary.setPatientId(event.getPatientId());
            summary.setPhysicianId(event.getPhysicianId());
            summary.setStatus("CANCELED");
            log.warn("⚠️ [Query Side] Appointment não encontrado no write model, usando apenas dados do evento");
        }

        queryRepository.save(summary);

        log.info("✅ [Query Side] Appointment cancelado no MongoDB: {} (status: CANCELED, preservando todos os campos)", summary.getId());
    }
}

