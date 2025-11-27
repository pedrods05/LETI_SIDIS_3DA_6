package leti_sisdis_6.happhysicians.events;

import leti_sisdis_6.happhysicians.query.PhysicianQueryRepository;
import leti_sisdis_6.happhysicians.query.PhysicianSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PhysicianEventHandler {

    private final PhysicianQueryRepository queryRepository;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "q.physician.summary.updater", durable = "true"),
            exchange = @Exchange(value = "${hap.rabbitmq.exchange:hap-exchange}", type = "topic"),
            key = "physician.registered"
    ))
    public void handlePhysicianRegistered(PhysicianRegisteredEvent event) {
        System.out.println("📥 [Query Side] Recebi evento: " + event.getFullName());

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

        System.out.println("✅ [Query Side] Guardado no MongoDB: " + summary.getId());
    }
}

