package leti_sisdis_6.happhysicians.events;

import leti_sisdis_6.happhysicians.query.PhysicianQueryRepository;
import leti_sisdis_6.happhysicians.query.PhysicianSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PhysicianEventHandler {

    private final PhysicianQueryRepository queryRepository;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "q.physician.summary.updater", durable = "true"),
            exchange = @Exchange(value = "${hap.rabbitmq.exchange:hap-exchange}", type = "topic"),
            key = "physician.registered"
    ))
    public void handlePhysicianRegistered(PhysicianRegisteredEvent event) {
        log.info("📥 [Query Side] Recebi evento: {}", event.getFullName());

        PhysicianSummary summary = new PhysicianSummary(
                event.getPhysicianId(),
                event.getFullName(),
                event.getLicenseNumber(),
                event.getUsername(),
                event.getSpecialtyId(),
                event.getSpecialtyName(),
                event.getDepartmentId(),
                event.getDepartmentName()
        );

        queryRepository.save(summary);

        log.info("✅ [Query Side] Guardado no MongoDB: {}", summary.getId());
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "q.physician.summary.updater", durable = "true"),
            exchange = @Exchange(value = "${hap.rabbitmq.exchange:hap-exchange}", type = "topic"),
            key = "physician.updated"
    ))
    public void handlePhysicianUpdated(PhysicianUpdatedEvent event) {
        log.info("📥 [Query Side] Recebi evento PhysicianUpdated: {}", event.getPhysicianId());

        // Buscar summary existente ou criar novo
        PhysicianSummary summary = queryRepository.findById(event.getPhysicianId())
                .orElse(new PhysicianSummary());

        // Atualizar todos os campos
        summary.setId(event.getPhysicianId());
        summary.setFullName(event.getFullName());
        summary.setLicenseNumber(event.getLicenseNumber());
        summary.setUsername(event.getUsername());
        summary.setSpecialtyId(event.getSpecialtyId());
        summary.setSpecialtyName(event.getSpecialtyName());
        summary.setDepartmentId(event.getDepartmentId());
        summary.setDepartmentName(event.getDepartmentName());

        queryRepository.save(summary);

        log.info("✅ [Query Side] Physician atualizado no MongoDB: {}", summary.getId());
    }
}

