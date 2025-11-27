package leti_sisdis_6.happatients.event;

import leti_sisdis_6.happatients.query.PatientSummary;
import leti_sisdis_6.happatients.query.PatientQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PatientEventHandler {

    private final PatientQueryRepository queryRepository;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "q.patient.summary.updater", durable = "true"),
            exchange = @Exchange(value = "${hap.rabbitmq.exchange:hap-exchange}", type = "topic"),
            key = "patient.registered"
    ))
    public void handlePatientRegistered(PatientRegisteredEvent event) {
        System.out.println("📥 [Query Side] Recebi evento: " + event.getName());

        PatientSummary summary = new PatientSummary(
                event.getPatientId(),
                event.getName(),
                event.getEmail()
        );

        queryRepository.save(summary);

        System.out.println("✅ [Query Side] Guardado no MongoDB: " + summary.getId());
    }
}
