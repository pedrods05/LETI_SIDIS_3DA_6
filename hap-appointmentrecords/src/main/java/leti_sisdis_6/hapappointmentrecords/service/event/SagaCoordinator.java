package leti_sisdis_6.hapappointmentrecords.service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import leti_sisdis_6.hapappointmentrecords.model.SagaEvent;
import leti_sisdis_6.hapappointmentrecords.model.SagaStatus;
import leti_sisdis_6.hapappointmentrecords.repository.SagaEventRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class SagaCoordinator {

    private final SagaEventRepository sagaEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void recordStep(String appointmentId, String type, Object payload, String correlationId, boolean compensation) {
        try {
            String json = payload == null ? "{}" : objectMapper.writeValueAsString(payload);
            SagaStatus status = compensation ? SagaStatus.COMPENSATED : SagaStatus.IN_PROGRESS;
            SagaEvent event = SagaEvent.builder()
                    .id(UUID.randomUUID().toString())
                    .appointmentId(appointmentId)
                    .type(type)
                    .payloadJson(json)
                    .correlationId(correlationId)
                    .status(status)
                    .occurredAt(Instant.now())
                    .build();
            sagaEventRepository.save(event);
            log.info("Saga step recorded | type={} | compensation={} | correlationId={} | appointmentId={}", type, compensation, correlationId, appointmentId);
        } catch (Exception ex) {
            log.warn("Failed to record saga step | type={} | appointmentId={}", type, appointmentId, ex);
        }
    }

    @Transactional
    public void complete(String appointmentId, String correlationId) {
        List<SagaEvent> events = sagaEventRepository.findByAppointmentIdOrderByOccurredAtAsc(appointmentId);
        events.forEach(e -> {
            if (e.getStatus() == SagaStatus.IN_PROGRESS) {
                e.setStatus(SagaStatus.COMPLETED);
            }
        });
        sagaEventRepository.saveAll(events);
        log.info("Saga marked completed | appointmentId={} | correlationId={}", appointmentId, correlationId);
    }

    @Transactional
    public void compensate(String appointmentId, String reason, String correlationId) {
        recordStep(appointmentId, "COMPENSATION_TRIGGERED", reason, correlationId, true);
        List<SagaEvent> events = sagaEventRepository.findByAppointmentIdOrderByOccurredAtAsc(appointmentId);
        events.forEach(e -> {
            if (e.getStatus() == SagaStatus.IN_PROGRESS) {
                e.setStatus(SagaStatus.COMPENSATED);
            }
        });
        sagaEventRepository.saveAll(events);
        log.info("Saga marked compensated | appointmentId={} | correlationId={} | reason={}", appointmentId, correlationId, reason);
    }
}

