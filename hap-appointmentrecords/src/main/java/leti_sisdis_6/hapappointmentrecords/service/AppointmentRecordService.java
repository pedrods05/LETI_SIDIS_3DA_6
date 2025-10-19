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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentRecordService {
    private final AppointmentRecordRepository recordRepository;
    
    @Autowired
    private ExternalServiceClient externalServiceClient;

    @Transactional
    public AppointmentRecordResponse createRecord(String appointmentId, AppointmentRecordRequest request, String physicianId) {
        // Get appointment details from physicians service via HTTP
        Map<String, Object> appointmentData = externalServiceClient.getAppointmentById(appointmentId);
        
        // Extract physician ID from appointment data
        String appointmentPhysicianId = (String) appointmentData.get("physicianId");
        if (appointmentPhysicianId == null) {
            throw new NotFoundException("Appointment data is incomplete");
        }

        // Verify authorization
        if (!appointmentPhysicianId.equals(physicianId)) {
            throw new UnauthorizedException("You are not authorized to record details for this appointment");
        }

        // Check if record already exists
        if (recordRepository.findByAppointmentId(appointmentId).isPresent()) {
            throw new IllegalStateException("A record already exists for this appointment");
        }

        // Generate record ID
        String recordId = generateRecordId();

        // Create appointment record
        AppointmentRecord record = AppointmentRecord.builder()
                .recordId(recordId)
                .appointmentId(appointmentId)
                .diagnosis(request.getDiagnosis())
                .treatmentRecommendations(request.getTreatmentRecommendations())
                .prescriptions(request.getPrescriptions())
                .duration(request.getDuration())
                .build();

        recordRepository.save(record);

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

        // Get appointment details from physicians service via HTTP
        Map<String, Object> appointmentData = externalServiceClient.getAppointmentById(record.getAppointmentId());
        String patientId = (String) appointmentData.get("patientId");
        
        if (patientId == null) {
            throw new NotFoundException("Appointment data is incomplete");
        }

        // Check authorization for patients
        if ("PATIENT".equals(currentUser.getRole())) {
            if (!patientId.equals(currentUser.getId())) {
                throw new UnauthorizedException("You are not authorized to view this appointment record");
            }
        }

        // Get physician details from physicians service via HTTP
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
        // Get all appointment records for this patient
        List<AppointmentRecord> records = recordRepository.findByAppointmentIdIn(
            getAppointmentIdsForPatient(patientId)
        );

        return records.stream()
            .map(record -> {
                try {
                    // Get appointment details from physicians service via HTTP
                    Map<String, Object> appointmentData = externalServiceClient.getAppointmentById(record.getAppointmentId());
                    String physicianId = (String) appointmentData.get("physicianId");
                    
                    // Get physician details
                    String physicianName = "Unknown Physician";
                    if (physicianId != null) {
                        try {
                            Map<String, Object> physicianData = externalServiceClient.getPhysicianById(physicianId);
                            physicianName = physicianData != null ? (String) physicianData.get("fullName") : "Unknown Physician";
                        } catch (Exception e) {
                            // Log error but continue with default name
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
                    // Return record with minimal info if external service fails
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
        // This would need to be implemented to get appointment IDs for a patient
        // For now, we'll get all records and filter by patient ID from appointment data
        // In a real implementation, you might want to cache this or have a different approach
        return recordRepository.findAll().stream()
            .map(AppointmentRecord::getAppointmentId)
            .collect(Collectors.toList());
    }

    private String generateRecordId() {
        long count = recordRepository.count();
        return String.format("REC%02d", count + 1);
    }
}
