package leti_sisdis_6.hapappointmentrecords.service;

import leti_sisdis_6.hapappointmentrecords.dto.input.AppointmentRecordRequest;
import leti_sisdis_6.hapappointmentrecords.dto.output.AppointmentRecordResponse;
import leti_sisdis_6.hapappointmentrecords.dto.output.AppointmentRecordViewDTO;
import leti_sisdis_6.hapappointmentrecords.model.AppointmentRecord;
import leti_sisdis_6.hapappointmentrecords.repository.AppointmentRecordRepository;
import leti_sisdis_6.hapappointmentrecords.exceptions.NotFoundException;
import leti_sisdis_6.hapappointmentrecords.exceptions.UnauthorizedException;
import leti_sisdis_6.hapauth.usermanagement.User;
import leti_sisdis_6.hapauth.usermanagement.Role;

import leti_sisdis_6.happhysicians.model.Appointment;
import leti_sisdis_6.happhysicians.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentRecordService {
    private final AppointmentRepository appointmentRepository;
    private final AppointmentRecordRepository recordRepository;

    @Transactional
    public AppointmentRecordResponse createRecord(String appointmentId, AppointmentRecordRequest request, String physicianId) {

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment not found with id: " + appointmentId));


        if (!appointment.getPhysician().getPhysicianId().equals(physicianId)) {
            throw new UnauthorizedException("You are not authorized to record details for this appointment");
        }


        if (recordRepository.findByAppointment_AppointmentId(appointmentId).isPresent()) {
            throw new IllegalStateException("A record already exists for this appointment");
        }


        String recordId = generateRecordId();


        AppointmentRecord record = AppointmentRecord.builder()
                .recordId(recordId)
                .appointment(appointment)
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
    public AppointmentRecordViewDTO getAppointmentRecord(String recordId, User currentUser) {
        AppointmentRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new NotFoundException("Appointment record not found with id: " + recordId));


        if (currentUser.getRole() == Role.PATIENT) {
            if (!record.getAppointment().getPatientId().equals(currentUser.getId())) {
                throw new UnauthorizedException("You are not authorized to view this appointment record");
            }
        }

        return AppointmentRecordViewDTO.builder()
                .recordId(record.getRecordId())
                .appointmentId(record.getAppointment().getAppointmentId())
                .physicianName(record.getAppointment().getPhysician().getFullName())
                .diagnosis(record.getDiagnosis())
                .treatmentRecommendations(record.getTreatmentRecommendations())
                .prescriptions(record.getPrescriptions())
                .duration(record.getDuration())
                .build();
    }

    @Transactional(readOnly = true)
    public List<AppointmentRecordViewDTO> getPatientRecords(String patientId) {
        List<Appointment> appointments = appointmentRepository.findByPatientIdOrderByDateTimeDesc(patientId);

        return appointments.stream()
            .flatMap(appointment -> recordRepository.findByAppointment_AppointmentId(appointment.getAppointmentId()).stream())
            .map(record -> AppointmentRecordViewDTO.builder()
                .recordId(record.getRecordId())
                .appointmentId(record.getAppointment().getAppointmentId())
                .physicianName(record.getAppointment().getPhysician().getFullName())
                .diagnosis(record.getDiagnosis())
                .treatmentRecommendations(record.getTreatmentRecommendations())
                .prescriptions(record.getPrescriptions())
                .duration(record.getDuration())
                .build())
            .collect(Collectors.toList());
    }

    private String generateRecordId() {
        long count = recordRepository.count();
        return String.format("REC%02d", count + 1);
    }
}
