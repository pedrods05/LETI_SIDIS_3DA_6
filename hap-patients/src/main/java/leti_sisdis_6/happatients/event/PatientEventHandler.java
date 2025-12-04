package leti_sisdis_6.happatients.event;

import leti_sisdis_6.happatients.query.PatientSummary;
import leti_sisdis_6.happatients.query.PatientQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
@Component
@RequiredArgsConstructor
@Slf4j
public class PatientEventHandler {

    private final PatientQueryRepository queryRepository;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "q.patient.summary.updater", durable = "true"),
            exchange = @Exchange(value = "${hap.rabbitmq.exchange:hap-exchange}", type = "topic"),
            key = "patient.registered"
    ))
    public void handlePatientRegistered(PatientRegisteredEvent event) {
        log.info("📥 [Query Side] Evento recebido | ID: {} | Nome: {}", event.getPatientId(), event.getFullName());

        try {
        PatientSummary.AddressSummary addr = new PatientSummary.AddressSummary(
                event.getAddress().getStreet(),
                event.getAddress().getCity(),
                event.getAddress().getPostalCode(),
                event.getAddress().getCountry()
        );

        PatientSummary.InsuranceSummary ins = new PatientSummary.InsuranceSummary(
                event.getInsuranceInfo().getPolicyNumber(),
                event.getInsuranceInfo().getProvider(),
                event.getInsuranceInfo().getCoverageType()
        );

        PatientSummary summary = new PatientSummary(
                event.getPatientId(),
                event.getFullName(),
                event.getEmail(),
                event.getPhoneNumber(),
                event.getBirthDate(),
                event.getDataConsentGiven(),
                event.getDataConsentDate(),
                addr,
                ins,
                java.util.Collections.emptyList()
        );

        queryRepository.save(summary);

            log.info("✅ [Query Side] Sucesso | Mongo Document ID: {}", event.getPatientId());
        } catch (Exception e) {
                   log.error("❌ [Query Side] Falha ao processar evento ID: {}", event.getPatientId(), e);
        }
}}
