package com.pcm.psoft.pcmclinic_api.appointment.service;

import com.pcm.psoft.pcmclinic_api.appointment.dto.input.ScheduleAppointmentRequest;
import com.pcm.psoft.pcmclinic_api.appointment.dto.input.UpdateAppointmentRequest;
import com.pcm.psoft.pcmclinic_api.appointment.dto.output.AppointmentIdResponse;
import com.pcm.psoft.pcmclinic_api.appointment.dto.output.AppointmentListDTO;
import com.pcm.psoft.pcmclinic_api.appointment.dto.output.PhysicianAverageDTO;
import com.pcm.psoft.pcmclinic_api.appointment.api.AppointmentMapper;
import com.pcm.psoft.pcmclinic_api.appointment.model.Appointment;
import com.pcm.psoft.pcmclinic_api.appointment.model.AppointmentStatus;
import com.pcm.psoft.pcmclinic_api.appointment.repository.AppointmentRepository;
import com.pcm.psoft.pcmclinic_api.appointment.repository.AppointmentRecordRepository;
import com.pcm.psoft.pcmclinic_api.exceptions.NotFoundException;
import com.pcm.psoft.pcmclinic_api.patient.model.Patient;
import com.pcm.psoft.pcmclinic_api.patient.repository.PatientRepository;
import com.pcm.psoft.pcmclinic_api.usermanagement.model.Physician;
import com.pcm.psoft.pcmclinic_api.usermanagement.repository.PhysicianRepository;
import com.pcm.psoft.pcmclinic_api.usermanagement.model.User;
import com.pcm.psoft.pcmclinic_api.usermanagement.model.Role;
import com.pcm.psoft.pcmclinic_api.usermanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pcm.psoft.pcmclinic_api.appointment.util.SlotCalculator;
import com.pcm.psoft.pcmclinic_api.appointment.dto.output.AppointmentSlotDto;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.pcm.psoft.pcmclinic_api.appointment.util.AppointmentTimeValidator;
import com.pcm.psoft.pcmclinic_api.appointment.dto.output.AppointmentHistoryDTO;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final AppointmentRecordRepository recordRepository;
    private final PatientRepository patientRepository;
    private final PhysicianRepository physicianRepository;
    private final AppointmentMapper appointmentMapper;
    private final SlotCalculator slotCalculator;
    private final UserRepository userRepository;
    private final AppointmentTimeValidator timeValidator;

    @Transactional
    public AppointmentIdResponse scheduleAppointment(String patientEmail, ScheduleAppointmentRequest request) {
        User patientUser = userRepository.findByUsername(patientEmail)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        Patient patient = patientRepository.findByEmail(patientEmail)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        return createAppointment(patient, request);
    }

    @Transactional
    public AppointmentIdResponse scheduleAppointment(ScheduleAppointmentRequest request) {
        String patientId = request.getPatientId();
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found with ID: " + patientId));

        return createAppointment(patient, request);
    }

    private String generateAppointmentId() {
        List<Appointment> appointments = appointmentRepository.findAll();
        int lastId = 0;

        if (!appointments.isEmpty()) {
            // Encontrar o maior número de ID existente
            lastId = appointments.stream()
                .map(appointment -> {
                    String id = appointment.getAppointmentId();
                    if (id.startsWith("APT")) {
                        try {
                            return Integer.parseInt(id.substring(3));
                        } catch (NumberFormatException e) {
                            return 0;
                        }
                    }
                    return 0;
                })
                .max(Integer::compareTo)
                .orElse(0);
        }

        return String.format("APT%02d", lastId + 1);
    }

    private AppointmentIdResponse createAppointment(Patient patient, ScheduleAppointmentRequest request) {
        Physician physician = physicianRepository.findById(request.getPhysicianId())
                .orElseThrow(() -> new RuntimeException("Physician not found"));

        // Validate appointment time
        timeValidator.validateAppointmentTime(request.getDateTime());

        // Check if physician is available
        if (appointmentRepository.existsByPhysician_PhysicianIdAndDateTime(
                request.getPhysicianId(), request.getDateTime())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "The physician already has an appointment scheduled for this time slot. Please choose another time.");
        }

        // Create appointment
        Appointment appointment = new Appointment();
        appointment.setAppointmentId(generateAppointmentId());
        appointment.setPatient(patient);
        appointment.setPhysician(physician);
        appointment.setDateTime(request.getDateTime());
        appointment.setConsultationType(request.getConsultationType());
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        appointmentRepository.save(appointment);

        return new AppointmentIdResponse(
            appointment.getAppointmentId(),
            AppointmentStatus.SCHEDULED.name(),
            "Appointment scheduled successfully"
        );
    }

    @Transactional(readOnly = true)
    public Object getAppointmentById(String appointmentId, User currentUser) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment not found with id: " + appointmentId));

        return currentUser.getRole() == Role.ADMIN ?
               appointmentMapper.toDetailsDTO(appointment) :
               appointmentMapper.toPatientDTO(appointment);
    }

    @Transactional(readOnly = true)
    public List<AppointmentListDTO> listAppointmentsByPatient(String patientId) {
        List<Appointment> appointments = appointmentRepository.findByPatient_PatientId(patientId);
        return appointmentMapper.toListDTO(appointments);
    }

    @Transactional
    public Object updateAppointment(String appointmentId, UpdateAppointmentRequest request, User currentUser) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment not found with id: " + appointmentId));

        if (request.getDateTime() != null) {
            timeValidator.validateAppointmentTime(request.getDateTime());
            if (appointmentRepository.existsByPhysician_PhysicianIdAndDateTime(
                    appointment.getPhysician().getPhysicianId(), request.getDateTime())) {
                throw new IllegalArgumentException("Physician already has an appointment at this time");
            }
            appointment.setDateTime(request.getDateTime());
            appointment.setWasRescheduled(true);
        }

        if (request.getConsultationType() != null) {
            appointment.setConsultationType(request.getConsultationType());
        }

        appointment = appointmentRepository.save(appointment);
        return currentUser.getRole() == Role.ADMIN ?
               appointmentMapper.toDetailsDTO(appointment) :
               appointmentMapper.toPatientDTO(appointment);
    }

    @Transactional
    public Object cancelAppointment(String appointmentId, User currentUser) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment not found with id: " + appointmentId));

        if (appointment.getStatus() == AppointmentStatus.CANCELED) {
            throw new IllegalStateException("Appointment is already cancelled");
        }

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed appointment");
        }

        appointment.setStatus(AppointmentStatus.CANCELED);
        appointment = appointmentRepository.save(appointment);

        return currentUser.getRole() == Role.ADMIN ?
               appointmentMapper.toDetailsDTO(appointment) :
               appointmentMapper.toPatientDTO(appointment);
    }

    @Transactional(readOnly = true)
    public List<AppointmentListDTO> listUpcomingAppointments() {
        List<Appointment> appointments = appointmentRepository.findByDateTimeAfterOrderByDateTimeAsc(LocalDateTime.now());
        return appointmentMapper.toListDTO(appointments);
    }

    @Transactional(readOnly = true)
    public List<PhysicianAverageDTO> getAverageDurationsPerPhysician() {
        List<Object[]> results = appointmentRepository.findAverageDurationPerPhysician();
        return appointmentMapper.toAverageDTOList(results);
    }

    public List<AppointmentSlotDto> getAvailableSlots(String physicianId, LocalDate startDate) {
        Physician physician = physicianRepository.findById(physicianId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Physician not found"));
        LocalDate endDate = startDate.plusDays(10);
        List<Appointment> appointments = appointmentRepository.findByPhysician_PhysicianIdAndDateTimeBetween(
                physicianId,
                startDate.atStartOfDay(),
                endDate.atTime(LocalTime.MAX)
        );
        return slotCalculator.generateAvailableSlots(physician, appointments, startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<AppointmentHistoryDTO> getCompletedAppointmentsWithDetails(String patientId) {
        List<Appointment> appointments = appointmentRepository.findByPatient_PatientIdAndStatus(
            patientId,
            AppointmentStatus.COMPLETED
        );

        return appointments.stream()
            .map(this::toAppointmentHistoryDTO)
            .toList();
    }

    private AppointmentHistoryDTO toAppointmentHistoryDTO(Appointment appointment) {
        AppointmentHistoryDTO.AppointmentRecordDTO recordDTO = recordRepository
            .findByAppointment_AppointmentId(appointment.getAppointmentId())
            .map(record -> AppointmentHistoryDTO.AppointmentRecordDTO.builder()
                .diagnosis(record.getDiagnosis())
                .treatmentRecommendations(record.getTreatmentRecommendations())
                .prescriptions(record.getPrescriptions())
                .duration(record.getDuration())
                .build())
            .orElse(null);

        return AppointmentHistoryDTO.builder()
            .appointmentId(appointment.getAppointmentId())
            .dateTime(appointment.getDateTime())
            .consultationType(appointment.getConsultationType())
            .status(appointment.getStatus())
            .record(recordDTO)
            .build();
    }
}
