package leti_sisdis_6.hapappointmentrecords.service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import leti_sisdis_6.hapappointmentrecords.model.AppointmentRecordProjection;
import leti_sisdis_6.hapappointmentrecords.model.AppointmentStatus;
import leti_sisdis_6.hapappointmentrecords.model.EventStoreEntry;
import leti_sisdis_6.hapappointmentrecords.repository.AppointmentRecordProjectionRepository;
import leti_sisdis_6.hapappointmentrecords.repository.EventStoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventReplayer {

    private final EventStoreRepository eventStoreRepository;
    private final AppointmentRecordProjectionRepository projectionRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void rebuildProjections(String appointmentId) {
        List<EventStoreEntry> events = eventStoreRepository.findByAppointmentIdOrderByOccurredAtAsc(appointmentId);
        if (events.isEmpty()) {
            log.warn("No events found for appointment {} to rebuild projection", appointmentId);
            return;
        }
        AppointmentRecordProjection projection = null;
        AppointmentStatus status = null;
        for (EventStoreEntry e : events) {
            switch (e.getEventType()) {
                case "appointment.created" -> {
                    projection = applyCreated(e);
                    status = extractStatusFromCreated(e, status);
                }
                case "appointment.updated" -> {
                    if (projection != null) {
                        status = applyUpdated(projection, e, status);
                    }
                }
                case "appointment.canceled" -> {
                    if (projection != null) {
                        projection.setTreatmentRecommendations("Canceled: " + e.getPayload());
                    }
                }
                default -> log.debug("Skipping unknown event type {}", e.getEventType());
            }
        }
        if (projection != null) {
            projectionRepository.save(projection);
            log.info("Projection rebuilt for appointment {} with status {}", appointmentId, status);
        }
    }

    private AppointmentRecordProjection applyCreated(EventStoreEntry e) {
        try {
            AppointmentCreatedEvent ev = objectMapper.readValue(e.getPayload(), AppointmentCreatedEvent.class);
            return AppointmentRecordProjection.builder()
                    .recordId("REC" + ev.getAppointmentId())
                    .appointmentId(ev.getAppointmentId())
                    .patientId(ev.getPatientId())
                    .physicianId(ev.getPhysicianId())
                    .diagnosis(null)
                    .treatmentRecommendations(null)
                    .prescriptions(null)
                    .duration(null)
                    .build();
        } catch (Exception ex) {
            log.warn("Failed to apply created event for {}", e.getAppointmentId(), ex);
            return null;
        }
    }

    private AppointmentStatus applyUpdated(AppointmentRecordProjection current, EventStoreEntry e, AppointmentStatus previousStatus) {
        try {
            AppointmentUpdatedEvent ev = objectMapper.readValue(e.getPayload(), AppointmentUpdatedEvent.class);
            // Map status progression; leave clinical details as-is since events don't carry them
            return ev.getNewStatus() != null ? ev.getNewStatus() : previousStatus;
        } catch (Exception ex) {
            log.warn("Failed to apply updated event for {}", e.getAppointmentId(), ex);
            return previousStatus;
        }
    }

    private AppointmentStatus extractStatusFromCreated(EventStoreEntry e, AppointmentStatus fallback) {
        try {
            AppointmentCreatedEvent ev = objectMapper.readValue(e.getPayload(), AppointmentCreatedEvent.class);
            return ev.getStatus() != null ? ev.getStatus() : fallback;
        } catch (Exception ex) {
            log.warn("Failed to read status from created event {}", e.getAppointmentId(), ex);
            return fallback;
        }
    }
}
