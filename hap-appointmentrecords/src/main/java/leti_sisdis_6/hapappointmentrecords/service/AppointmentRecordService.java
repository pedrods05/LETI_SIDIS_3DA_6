package leti_sisdis_6.hapappointmentrecords.service;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import leti_sisdis_6.hapappointmentrecords.dto.input.AppointmentRecordRequest;
import leti_sisdis_6.hapappointmentrecords.dto.output.AppointmentRecordResponse;
import leti_sisdis_6.hapappointmentrecords.dto.output.AppointmentRecordViewDTO;
import leti_sisdis_6.hapappointmentrecords.dto.local.UserDTO;
import leti_sisdis_6.hapappointmentrecords.exceptions.NotFoundException;
import leti_sisdis_6.hapappointmentrecords.exceptions.UnauthorizedException;
import leti_sisdis_6.hapappointmentrecords.http.ExternalServiceClient;
import leti_sisdis_6.hapappointmentrecords.model.AppointmentRecord;
import leti_sisdis_6.hapappointmentrecords.model.AppointmentRecordProjection;
import leti_sisdis_6.hapappointmentrecords.repository.AppointmentRecordRepository;
import leti_sisdis_6.hapappointmentrecords.repository.AppointmentRecordProjectionRepository;
import leti_sisdis_6.hapappointmentrecords.service.event.AppointmentEventsPublisher;
import leti_sisdis_6.hapappointmentrecords.service.event.AppointmentRecordCreatedEvent;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentRecordService {

    private final AppointmentRecordRepository recordRepository;
    private final ExternalServiceClient externalServiceClient;
    private final AppointmentEventsPublisher eventsPublisher;
    private final AppointmentRecordProjectionRepository recordProjectionRepository; // record read-model (by recordId)

    @Transactional
    @Timed(value = "appointment.record.create.time", description = "Time taken to create an appointment record")
    @Counted(value = "appointment.record.create.count", description = "Number of appointment records created")
    public AppointmentRecordResponse createRecord(String appointmentId,
                                                  AppointmentRecordRequest request,
                                                  String physicianId) {

        // 1) Detalhes da consulta (serviço externo hap-physicians)
        Map<String, Object> appointmentData = externalServiceClient.getAppointmentById(appointmentId);

        String appointmentPhysicianId = (String) appointmentData.get("physicianId");
        if (appointmentPhysicianId == null) {
            Object physObj = appointmentData.get("physician");
            if (physObj instanceof Map<?,?> physMap) {
                Object nestedId = physMap.get("physicianId");
                if (nestedId instanceof String pid && !pid.isBlank()) {
                    appointmentPhysicianId = pid;
                }
            }
        }
        if (appointmentPhysicianId == null) {
            throw new NotFoundException("Appointment data is incomplete");
        }

        // 2) Autorização
        if (!appointmentPhysicianId.equals(physicianId)) {
            throw new UnauthorizedException("You are not authorized to record details for this appointment");
        }

        // 3) Evitar duplicado
        if (recordRepository.findByAppointmentId(appointmentId).isPresent()) {
            throw new IllegalStateException("Record already exists for appointment " + appointmentId);
        }

        // 4) Extract data from physicians service response
        String patientId = (String) appointmentData.get("patientId");
        if (patientId == null) {
            throw new NotFoundException("Appointment data is incomplete - missing patientId");
        }

        // 5) Criar o record (usando apenas appointmentId)
        String recordId = generateRecordId();

        AppointmentRecord record = AppointmentRecord.builder()
                .recordId(recordId)
                .appointmentId(appointmentId) // store ID, not entity
                .diagnosis(request.getDiagnosis())
                .treatmentRecommendations(request.getTreatmentRecommendations())
                .prescriptions(request.getPrescriptions())
                .duration(request.getDuration())
                .build();

        recordRepository.save(record);

        // Persist read-model projection for fast queries by recordId
        AppointmentRecordProjection recordProjection = AppointmentRecordProjection.builder()
                .recordId(recordId)
                .appointmentId(appointmentId)
                .patientId(patientId)
                .physicianId(appointmentPhysicianId)
                .diagnosis(record.getDiagnosis())
                .treatmentRecommendations(record.getTreatmentRecommendations())
                .prescriptions(record.getPrescriptions())
                .duration(record.getDuration())
                .build();
        recordProjectionRepository.save(recordProjection);

        // Optional event for record creation (disabled by default)
        eventsPublisher.publishAppointmentRecordCreated(new AppointmentRecordCreatedEvent(
                recordId,
                appointmentId,
                patientId,
                appointmentPhysicianId,
                LocalDateTime.now()
        ));

        // 6) Resposta
        return AppointmentRecordResponse.builder()
                .message("Appointment record created successfully.")
                .appointmentId(appointmentId)
                .recordId(recordId)
                .build();
    }

    @Transactional(readOnly = true)
    public AppointmentRecordViewDTO getAppointmentRecord(String recordId, UserDTO currentUser) {
        // Use record projection read model (Mongo) keyed by recordId
        AppointmentRecordProjection projection = recordProjectionRepository.findById(recordId)
                .orElseThrow(() -> new NotFoundException("Appointment record not found with id: " + recordId));

        // Autorização para pacientes (usa projection.patientId)
        if ("PATIENT".equals(currentUser.getRole()) && !projection.getPatientId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You are not authorized to view this appointment record");
        }

        // Nome do médico (serviço externo) — opcional, mantemos para enriquecer a resposta
        String physicianName = "Unknown Physician";
        String physicianId = projection.getPhysicianId();
        if (physicianId != null) {
            try {
                Map<String, Object> physicianData = externalServiceClient.getPhysicianById(physicianId);
                physicianName = physicianData != null ? (String) physicianData.getOrDefault("fullName", physicianName) : physicianName;
            } catch (Exception ignored) {}
        }

        return AppointmentRecordViewDTO.builder()
                .recordId(projection.getRecordId())
                .appointmentId(projection.getAppointmentId())
                .physicianName(physicianName)
                .diagnosis(projection.getDiagnosis())
                .treatmentRecommendations(projection.getTreatmentRecommendations())
                .prescriptions(projection.getPrescriptions())
                .duration(projection.getDuration())
                .build();
    }

    @Transactional(readOnly = true)
    public List<AppointmentRecordViewDTO> getPatientRecords(String patientId) {
        // Ler todas as projeções de records e filtrar por patientId
        return recordProjectionRepository.findByPatientId(patientId).stream()
                .map(p -> {
                    String physicianName = "Unknown Physician";
                    String physicianId = p.getPhysicianId();
                    if (physicianId != null) {
                        try {
                            Map<String, Object> physicianData = externalServiceClient.getPhysicianById(physicianId);
                            physicianName = physicianData != null ? (String) physicianData.getOrDefault("fullName", physicianName) : physicianName;
                        } catch (Exception ignored) {}
                    }
                    return AppointmentRecordViewDTO.builder()
                            .recordId(p.getRecordId())
                            .appointmentId(p.getAppointmentId())
                            .physicianName(physicianName)
                            .diagnosis(p.getDiagnosis())
                            .treatmentRecommendations(p.getTreatmentRecommendations())
                            .prescriptions(p.getPrescriptions())
                            .duration(p.getDuration())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private String generateRecordId() {
        // usa UUID para evitar colisão quando há deletes
        return "REC" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}