package leti_sisdis_6.hapappointmentrecords.service;

import leti_sisdis_6.hapappointmentrecords.dto.input.AppointmentRecordRequest;
import leti_sisdis_6.hapappointmentrecords.dto.output.AppointmentRecordResponse;
import leti_sisdis_6.hapappointmentrecords.dto.output.AppointmentRecordViewDTO;
import leti_sisdis_6.hapappointmentrecords.dto.local.UserDTO;
import leti_sisdis_6.hapappointmentrecords.model.AppointmentRecord;
import leti_sisdis_6.hapappointmentrecords.repository.AppointmentRecordRepository;
import leti_sisdis_6.hapappointmentrecords.exceptions.NotFoundException;
import leti_sisdis_6.hapappointmentrecords.exceptions.UnauthorizedException;
import leti_sisdis_6.hapappointmentrecords.http.ExternalServiceClient;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;           // <-- para a lista hardcoded
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentRecordService {

    private final AppointmentRecordRepository recordRepository;

    @Autowired
    private ExternalServiceClient externalServiceClient;

    // ========= HARD-CODED PEERS (apenas appointmentrecords) =========
    // Instâncias conhecidas do MESMO módulo (appointmentsrecords):
    // instance1: 8083, instance2: 8090
    private final List<String> peers = Arrays.asList(
            "http://localhost:8083",
            "http://localhost:8090"
    );

    // porta desta instância (para excluir-se a si própria ao “broadcastar”)
    @Value("${server.port}")
    private String currentPort;
    // ================================================================

    @Transactional
    public AppointmentRecordResponse createRecord(String appointmentId, AppointmentRecordRequest request, String physicianId) {
        Map<String, Object> appointmentData = externalServiceClient.getAppointmentById(appointmentId);

        String appointmentPhysicianId = (String) appointmentData.get("physicianId");
        if (appointmentPhysicianId == null) {
            throw new NotFoundException("Appointment data is incomplete");
        }

        if (!appointmentPhysicianId.equals(physicianId)) {
            throw new UnauthorizedException("You are not authorized to record details for this appointment");
        }

        if (recordRepository.findByAppointmentId(appointmentId).isPresent()) {
            throw new IllegalStateException("A record already exists for this appointment");
        }

        String recordId = generateRecordId();

        AppointmentRecord record = AppointmentRecord.builder()
                .recordId(recordId)
                .appointmentId(appointmentId)
                .diagnosis(request.getDiagnosis())
                .treatmentRecommendations(request.getTreatmentRecommendations())
                .prescriptions(request.getPrescriptions())
                .duration(request.getDuration())
                .build();

        recordRepository.save(record);

        // (Opcional) exemplo de uso dos peers hardcoded:
        // notifyPeers("/api/appointment-records/sync", Map.of("recordId", recordId));

        return AppointmentRecordResponse.builder()
                .message("Appointment record created successfully.")
                .appointmentId(appointmentId)
                .recordId(recordId)
                .build();
    }

    @Transactional(readOnly = true)
    public AppointmentRecordViewDTO getAppointmentRecord(String recordId, UserDTO currentUser) {
        AppointmentRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new NotFoundException("Appointment record not found with id: " + recordId));

        Map<String, Object> appointmentData = externalServiceClient.getAppointmentById(record.getAppointmentId());
        String patientId = (String) appointmentData.get("patientId");

        if (patientId == null) {
            throw new NotFoundException("Appointment data is incomplete");
        }

        if ("PATIENT".equals(currentUser.getRole())) {
            if (!patientId.equals(currentUser.getId())) {
                throw new UnauthorizedException("You are not authorized to view this appointment record");
            }
        }

        String physicianId = (String) appointmentData.get("physicianId");
        Map<String, Object> physicianData = externalServiceClient.getPhysicianById(physicianId);
        String physicianName = physicianData != null ? (String) physicianData.get("fullName") : "Unknown Physician";

        return AppointmentRecordViewDTO.builder()
                .recordId(record.getRecordId())
                .appointmentId(record.getAppointmentId())
                .physicianName(physicianName)
                .diagnosis(record.getDiagnosis())
                .treatmentRecommendations(record.getTreatmentRecommendations())
                .prescriptions(record.getPrescriptions())
                .duration(record.getDuration())
                .build();
    }

    @Transactional(readOnly = true)
    public List<AppointmentRecordViewDTO> getPatientRecords(String patientId) {
        List<AppointmentRecord> records = recordRepository.findByAppointmentIdIn(
                getAppointmentIdsForPatient(patientId)
        );

        return records.stream()
                .map(record -> {
                    try {
                        Map<String, Object> appointmentData = externalServiceClient.getAppointmentById(record.getAppointmentId());
                        String physicianId = (String) appointmentData.get("physicianId");

                        String physicianName = "Unknown Physician";
                        if (physicianId != null) {
                            try {
                                Map<String, Object> physicianData = externalServiceClient.getPhysicianById(physicianId);
                                physicianName = physicianData != null ? (String) physicianData.get("fullName") : "Unknown Physician";
                            } catch (Exception ignored) {
                                physicianName = "Unknown Physician";
                            }
                        }

                        return AppointmentRecordViewDTO.builder()
                                .recordId(record.getRecordId())
                                .appointmentId(record.getAppointmentId())
                                .physicianName(physicianName)
                                .diagnosis(record.getDiagnosis())
                                .treatmentRecommendations(record.getTreatmentRecommendations())
                                .prescriptions(record.getPrescriptions())
                                .duration(record.getDuration())
                                .build();
                    } catch (Exception e) {
                        return AppointmentRecordViewDTO.builder()
                                .recordId(record.getRecordId())
                                .appointmentId(record.getAppointmentId())
                                .physicianName("Unknown Physician")
                                .diagnosis(record.getDiagnosis())
                                .treatmentRecommendations(record.getTreatmentRecommendations())
                                .prescriptions(record.getPrescriptions())
                                .duration(record.getDuration())
                                .build();
                    }
                })
                .collect(Collectors.toList());
    }

    private List<String> getAppointmentIdsForPatient(String patientId) {
        return recordRepository.findAll().stream()
                .map(AppointmentRecord::getAppointmentId)
                .collect(Collectors.toList());
    }

    private String generateRecordId() {
        long count = recordRepository.count();
        return String.format("REC%02d", count + 1);
    }

    // ===== Helpers (opcional): filtrar peers e “broadcast” =====
    private List<String> filteredPeers() {
        // exclui a própria instância com base na porta atual
        return peers.stream()
                .filter(p -> currentPort == null || !p.endsWith(":" + currentPort))
                .toList();
    }

    // Exemplo de uso se quiseres notificar os outros nós (usa o teu ExternalServiceClient/RestTemplate)
    // private void notifyPeers(String relativePath, Object payload) {
    //     for (String peer : filteredPeers()) {
    //         try {
    //             externalServiceClient.getRestTemplate()
    //                 .postForLocation(peer + (relativePath.startsWith("/") ? relativePath : "/" + relativePath), payload);
    //         } catch (Exception ignored) {}
    //     }
    // }
}
