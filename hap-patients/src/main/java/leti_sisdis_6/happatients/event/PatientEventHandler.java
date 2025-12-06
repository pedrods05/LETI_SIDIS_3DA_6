package leti_sisdis_6.happatients.event;

import leti_sisdis_6.happatients.query.PatientSummary;
import leti_sisdis_6.happatients.query.PatientQueryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import static leti_sisdis_6.happatients.config.RabbitMQConfig.CORRELATION_ID_HEADER;

@Component
@RequiredArgsConstructor
@Slf4j
public class PatientEventHandler {

    private final PatientQueryRepository queryRepository;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "q.patient.summary.updater.${spring.profiles.active}", durable = "true"),
            exchange = @Exchange(value = "${hap.rabbitmq.exchange:hap-exchange}", type = "topic"),
            key = "patient.registered"
    ))
    public void handlePatientRegistered(PatientRegisteredEvent event,
                                        Message message,
                                        @Header(name = CORRELATION_ID_HEADER, required = false) String correlationIdHeader) {

        if (correlationIdHeader == null || correlationIdHeader.isBlank()) {
            Object headerVal = message.getMessageProperties().getHeaders().get(CORRELATION_ID_HEADER);
            if (headerVal != null) {
                correlationIdHeader = headerVal.toString();
            }
        }
        if (correlationIdHeader != null && !correlationIdHeader.isBlank()) {
            MDC.put(CORRELATION_ID_HEADER, correlationIdHeader);
        }

        log.info("📥 [Query Side] Evento recebido | correlationId={} | ID={} | Nome={}",
                correlationIdHeader, event.getPatientId(), event.getFullName());

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

            log.info("✅ [Query Side] Sucesso | correlationId={} | Mongo Document ID={}",
                    correlationIdHeader, event.getPatientId());
        } catch (Exception e) {
            log.error("❌ [Query Side] Falha ao processar evento | correlationId={} | ID={}",
                    correlationIdHeader, event.getPatientId(), e);
        } finally {
            MDC.remove(CORRELATION_ID_HEADER);
        }
    }
}
