package leti_sisdis_6.hapappointmentrecords.service;

import leti_sisdis_6.hapappointmentrecords.dto.input.AppointmentRecordRequest;
import leti_sisdis_6.hapappointmentrecords.dto.output.AppointmentRecordResponse;
import leti_sisdis_6.hapappointmentrecords.dto.output.AppointmentRecordViewDTO;
import leti_sisdis_6.hapappointmentrecords.dto.local.UserDTO;
import leti_sisdis_6.hapappointmentrecords.exceptions.NotFoundException;
import leti_sisdis_6.hapappointmentrecords.exceptions.UnauthorizedException;
import leti_sisdis_6.hapappointmentrecords.http.ExternalServiceClient;
import leti_sisdis_6.hapappointmentrecords.model.Appointment;
import leti_sisdis_6.hapappointmentrecords.model.AppointmentRecord;
import leti_sisdis_6.hapappointmentrecords.repository.AppointmentRecordRepository;
import leti_sisdis_6.hapappointmentrecords.repository.AppointmentRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentRecordService {

    private final AppointmentRecordRepository recordRepository;
    private final AppointmentRepository appointmentRepository;
    private final ExternalServiceClient externalServiceClient;

    @Transactional
    public AppointmentRecordResponse createRecord(String appointmentId,
                                                  AppointmentRecordRequest request,
                                                  String physicianId) {

        // 1) Detalhes da consulta (serviço externo hap-physicians)
        Map<String, Object> appointmentData = externalServiceClient.getAppointmentById(appointmentId);

        String appointmentPhysicianId = (String) appointmentData.get("physicianId");
        if (appointmentPhysicianId == null) {
            throw new NotFoundException("Appointment data is incomplete");
        }

        // 2) Autorização
        if (!appointmentPhysicianId.equals(physicianId)) {
            throw new UnauthorizedException("You are not authorized to record details for this appointment");
        }

        // 3) Evitar duplicado (via propriedade aninhada)
        if (recordRepository.findByAppointment_AppointmentId(appointmentId).isPresent()) {
            throw new IllegalStateException("Record already exists for appointment " + appointmentId);
        }

        // 4) Carregar a entidade Appointment (do próprio serviço)
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment not found: " + appointmentId));

        // 5) Criar o record (ligando a ENTIDADE, não o ID)
        String recordId = generateRecordId();

        AppointmentRecord record = AppointmentRecord.builder()
                .recordId(recordId)
                .appointment(appointment) // relação 1–1
                .diagnosis(request.getDiagnosis())
                .treatmentRecommendations(request.getTreatmentRecommendations())
                .prescriptions(request.getPrescriptions())
                .duration(request.getDuration())
                .build();

        recordRepository.save(record);

        // 6) Resposta
        return AppointmentRecordResponse.builder()
                .message("Appointment record created successfully.")
                .appointmentId(appointment.getAppointmentId())
                .recordId(recordId)
                .build();
    }

    @Transactional(readOnly = true)
    public AppointmentRecordViewDTO getAppointmentRecord(String recordId, UserDTO currentUser) {
        AppointmentRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new NotFoundException("Appointment record not found with id: " + recordId));

        String appointmentId = record.getAppointment().getAppointmentId();

        // Dados da consulta via serviço externo
        Map<String, Object> appointmentData = externalServiceClient.getAppointmentById(appointmentId);
        String patientId = (String) appointmentData.get("patientId");
        if (patientId == null) {
            throw new NotFoundException("Appointment data is incomplete");
        }

        // Autorização para pacientes
        if ("PATIENT".equals(currentUser.getRole()) && !patientId.equals(currentUser.getId())) {
            throw new UnauthorizedException("You are not authorized to view this appointment record");
        }

        // Nome do médico (serviço externo)
        String physicianId = (String) appointmentData.get("physicianId");
        String physicianName = "Unknown Physician";
        if (physicianId != null) {
            try {
                Map<String, Object> physicianData = externalServiceClient.getPhysicianById(physicianId);
                physicianName = physicianData != null ? (String) physicianData.get("fullName") : "Unknown Physician";
            } catch (Exception ignored) {}
        }

        return AppointmentRecordViewDTO.builder()
                .recordId(record.getRecordId())
                .appointmentId(appointmentId)
                .physicianName(physicianName)
                .diagnosis(record.getDiagnosis())
                .treatmentRecommendations(record.getTreatmentRecommendations())
                .prescriptions(record.getPrescriptions())
                .duration(record.getDuration())
                .build();
    }

    @Transactional(readOnly = true)
    public List<AppointmentRecordViewDTO> getPatientRecords(String patientId) {
        // Estratégia simples: obter todos os records e filtrar pelos que pertencem ao patient
        // (usamos serviço externo para ler patientId da consulta)
        return recordRepository.findAll().stream()
                .map(record -> {
                    String appointmentId = record.getAppointment().getAppointmentId();
                    try {
                        Map<String, Object> appointmentData = externalServiceClient.getAppointmentById(appointmentId);
                        String apptPatientId = (String) appointmentData.get("patientId");
                        if (!patientId.equals(apptPatientId)) {
                            return null; // não é deste paciente
                        }

                        // nome do médico
                        String physicianName = "Unknown Physician";
                        String physicianId = (String) appointmentData.get("physicianId");
                        if (physicianId != null) {
                            try {
                                Map<String, Object> physicianData = externalServiceClient.getPhysicianById(physicianId);
                                physicianName = physicianData != null ? (String) physicianData.get("fullName") : "Unknown Physician";
                            } catch (Exception ignored) {}
                        }

                        return AppointmentRecordViewDTO.builder()
                                .recordId(record.getRecordId())
                                .appointmentId(appointmentId)
                                .physicianName(physicianName)
                                .diagnosis(record.getDiagnosis())
                                .treatmentRecommendations(record.getTreatmentRecommendations())
                                .prescriptions(record.getPrescriptions())
                                .duration(record.getDuration())
                                .build();

                    } catch (Exception e) {
                        // Se o serviço externo falhar, ignoramos este registo
                        return null;
                    }
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }

    private String generateRecordId() {
        // usa UUID para evitar colisão quando há deletes
        return "REC" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
