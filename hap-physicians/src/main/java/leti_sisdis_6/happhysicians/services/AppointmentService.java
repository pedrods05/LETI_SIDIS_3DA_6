package leti_sisdis_6.happhysicians.services;

import leti_sisdis_6.happhysicians.api.AppointmentMapper;
import leti_sisdis_6.happhysicians.dto.input.ScheduleAppointmentRequest;
import leti_sisdis_6.happhysicians.dto.input.UpdateAppointmentRequest;
import leti_sisdis_6.happhysicians.dto.output.AppointmentDetailsDTO;
import leti_sisdis_6.happhysicians.dto.output.AppointmentListDTO;
import leti_sisdis_6.happhysicians.exceptions.AppointmentRecordNotFoundException;
import leti_sisdis_6.happhysicians.exceptions.PatientNotFoundException;
import leti_sisdis_6.happhysicians.exceptions.MicroserviceCommunicationException;
import leti_sisdis_6.happhysicians.model.Appointment;
import leti_sisdis_6.happhysicians.model.AppointmentStatus;
import leti_sisdis_6.happhysicians.model.ConsultationType;
import leti_sisdis_6.happhysicians.model.Physician;
import leti_sisdis_6.happhysicians.repository.AppointmentRepository;
import leti_sisdis_6.happhysicians.repository.PhysicianRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PhysicianRepository physicianRepository;

    @Autowired
    private ExternalServiceClient externalServiceClient;

    @Autowired
    private final AppointmentMapper appointmentMapper;

    public AppointmentService(AppointmentMapper appointmentMapper) {
        this.appointmentMapper = appointmentMapper;
    }


    public Appointment createAppointment(ScheduleAppointmentRequest dto) {
        Optional<Physician> physician = physicianRepository.findById(dto.getPhysicianId());
        if (physician.isEmpty()) {
            throw new RuntimeException("Physician not found");
        }

        Map<String, Object> appointmentData = new HashMap<>();
        appointmentData.put("appointmentId", dto.getAppointmentId());
        appointmentData.put("patientId", dto.getPatientId());
        appointmentData.put("physicianId", dto.getPhysicianId());
        appointmentData.put("dateTime", dto.getDateTime().toString());
        appointmentData.put("consultationType", dto.getConsultationType().toString());
        appointmentData.put("status", dto.getStatus().toString());

        try {
            externalServiceClient.createAppointmentInRecords(appointmentData);

            Appointment.AppointmentBuilder appointmentBuilder = Appointment.builder()
                    .appointmentId(dto.getAppointmentId())
                    .patientId(dto.getPatientId())
                    .physician(physician.get())
                    .dateTime(dto.getDateTime())
                    .consultationType(dto.getConsultationType())
                    .status(dto.getStatus())
                    .wasRescheduled(dto.getWasRescheduled() != null ? dto.getWasRescheduled() : false);

            try {
                Map<String, Object> patientData = externalServiceClient.getPatientById(dto.getPatientId());
                appointmentBuilder.patientName((String) patientData.get("fullName"));
                appointmentBuilder.patientEmail((String) patientData.get("email"));
                appointmentBuilder.patientPhone((String) patientData.get("phoneNumber"));
            } catch (Exception e) {
                System.out.println("Warning: Could not fetch patient data for ID " + dto.getPatientId() + ": " + e.getMessage());
            }

            return appointmentRepository.save(appointmentBuilder.build());
        } catch (MicroserviceCommunicationException e) {
            String msg = e.getMessage();
            throw new RuntimeException(msg.contains("conflict") || msg.contains("exists")
                ? "Physician already has an appointment at this time or appointment ID already exists"
                : "Failed to create appointment: " + msg);
        }
    }
    @Transactional(readOnly = true)
    public List<AppointmentListDTO> listUpcomingAppointments() {
        List<Appointment> upcoming = appointmentRepository
                .findByDateTimeAfterOrderByDateTimeAsc(LocalDateTime.now())
                .stream()
                .filter(a -> a.getStatus() == AppointmentStatus.SCHEDULED)
                .toList();
        for (Appointment a : upcoming) {
            if (a.getPatientName() == null || a.getPatientEmail() == null || a.getPatientPhone() == null) {
                try {
                    Map<String, Object> patientData = externalServiceClient.getPatientById(a.getPatientId());
                    a.setPatientName((String) patientData.get("fullName"));
                    a.setPatientEmail((String) patientData.get("email"));
                    a.setPatientPhone((String) patientData.get("phoneNumber"));
                } catch (Exception ignored) {}
            }
        }
        return appointmentMapper.toListDTO(upcoming);
    }

    public List<Appointment> getAllAppointments() {
        List<Appointment> remote = externalServiceClient.listAppointments().stream()
                .map(this::mapRemoteAppointment)
                .collect(Collectors.toList());
        List<Appointment> localScheduled = appointmentRepository
                .findByDateTimeAfterOrderByDateTimeAsc(LocalDateTime.now()).stream()
                .filter(a -> a.getStatus() == AppointmentStatus.SCHEDULED)
                .collect(Collectors.toList());
        for (Appointment a : localScheduled) {
            if (a.getPatientName() == null || a.getPatientEmail() == null || a.getPatientPhone() == null) {
                try {
                    Map<String, Object> patientData = externalServiceClient.getPatientById(a.getPatientId());
                    a.setPatientName((String) patientData.get("fullName"));
                    a.setPatientEmail((String) patientData.get("email"));
                    a.setPatientPhone((String) patientData.get("phoneNumber"));
                } catch (Exception ignored) {}
            }
        }
        Map<String, Appointment> merged = new LinkedHashMap<>();
        for (Appointment r : remote) merged.put(r.getAppointmentId(), r);
        for (Appointment l : localScheduled) merged.put(l.getAppointmentId(), l);
        return new ArrayList<>(merged.values());
    }

    public List<Appointment> getAppointmentsByPhysician(String physicianId) {
        return externalServiceClient.listAppointmentsByPhysician(physicianId).stream()
                .map(this::mapRemoteAppointment)
                .collect(Collectors.toList());
    }

    public List<Appointment> getAppointmentsByPatient(String patientId) {
        return externalServiceClient.listAppointmentsByPatient(patientId).stream()
                .map(this::mapRemoteAppointment)
                .collect(Collectors.toList());
    }

    public Optional<Appointment> getAppointmentById(String appointmentId) {
        try {
            Map<String, Object> m = externalServiceClient.getAppointment(appointmentId);
            return Optional.of(mapRemoteAppointment(m));
        } catch (Exception e) {
            Optional<Appointment> local = appointmentRepository.findById(appointmentId);
            local.ifPresent(a -> {
                if (a.getPatientName() == null || a.getPatientEmail() == null || a.getPatientPhone() == null) {
                    try {
                        Map<String, Object> patientData = externalServiceClient.getPatientById(a.getPatientId());
                        a.setPatientName((String) patientData.get("fullName"));
                        a.setPatientEmail((String) patientData.get("email"));
                        a.setPatientPhone((String) patientData.get("phoneNumber"));
                    } catch (Exception ignored) {}
                }
            });
            return local;
        }
    }

    public List<Appointment> getAppointmentsByDateRange(LocalDateTime start, LocalDateTime end) {
        return appointmentRepository.findByDateTimeBetween(start, end);
    }

    public Appointment updateAppointment(String appointmentId, UpdateAppointmentRequest dto) {
        Optional<Appointment> optionalAppointment = appointmentRepository.findById(appointmentId);
        if (optionalAppointment.isEmpty()) {
            return null;
        }

        Appointment appointment = optionalAppointment.get();

        final String physicianIdToUse;
        final Physician physicianToSet;

        if (dto.getPhysicianId() != null) {
            physicianIdToUse = dto.getPhysicianId();
            physicianToSet = physicianRepository.findById(physicianIdToUse)
                    .orElseThrow(() -> new RuntimeException("Physician not found: " + physicianIdToUse));
        } else {
            physicianToSet = appointment.getPhysician();
            physicianIdToUse = physicianToSet.getPhysicianId();
        }

        Map<String, Object> appointmentData = new HashMap<>();
        appointmentData.put("patientId", dto.getPatientId() != null ? dto.getPatientId() : appointment.getPatientId());
        appointmentData.put("physicianId", physicianIdToUse);
        appointmentData.put("dateTime", (dto.getDateTime() != null ? dto.getDateTime() : appointment.getDateTime()).toString());
        appointmentData.put("consultationType", (dto.getConsultationType() != null ? dto.getConsultationType() : appointment.getConsultationType()).toString());
        appointmentData.put("status", (dto.getStatus() != null ? dto.getStatus() : appointment.getStatus()).toString());

        try {
            externalServiceClient.updateAppointmentInRecords(appointmentId, appointmentData);

            if (dto.getPatientId() != null) appointment.setPatientId(dto.getPatientId());
            appointment.setPhysician(physicianToSet);
            if (dto.getDateTime() != null) appointment.setDateTime(dto.getDateTime());
            if (dto.getConsultationType() != null) appointment.setConsultationType(dto.getConsultationType());
            if (dto.getStatus() != null) appointment.setStatus(dto.getStatus());
            if (dto.getWasRescheduled() != null) appointment.setWasRescheduled(dto.getWasRescheduled());

            return appointmentRepository.save(appointment);
        } catch (MicroserviceCommunicationException e) {
            throw new RuntimeException("Failed to update appointment: " + e.getMessage());
        }
    }

    public boolean deleteAppointment(String appointmentId) {
        if (appointmentRepository.existsById(appointmentId)) {
            try {
                externalServiceClient.deleteAppointmentInRecords(appointmentId);

                appointmentRepository.deleteById(appointmentId);
                return true;
            } catch (MicroserviceCommunicationException e) {
                appointmentRepository.deleteById(appointmentId);
                return true;
            }
        }
        return false;
    }


    public AppointmentDetailsDTO getAppointmentWithPatient(String appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        try {
            Map<String, Object> patientData = externalServiceClient.getPatientById(appointment.getPatientId());
            appointment.setPatientName((String) patientData.get("fullName"));
            appointment.setPatientEmail((String) patientData.get("email"));
            appointment.setPatientPhone((String) patientData.get("phoneNumber"));
            return new AppointmentDetailsDTO(appointment);
        } catch (PatientNotFoundException e) {
            return new AppointmentDetailsDTO(appointment);
        }
    }

    public AppointmentDetailsDTO getAppointmentWithPatientAndRecord(String appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        try {
            Map<String, Object> patientData = externalServiceClient.getPatientById(appointment.getPatientId());
            appointment.setPatientName((String) patientData.get("fullName"));
            appointment.setPatientEmail((String) patientData.get("email"));
            appointment.setPatientPhone((String) patientData.get("phoneNumber"));
        } catch (PatientNotFoundException e) {
        }

        try {
            Map<String, Object> appointmentRecord = externalServiceClient.getAppointmentRecord(appointmentId);
        } catch (AppointmentRecordNotFoundException e) {
        }

        return new AppointmentDetailsDTO(appointment);
    }

    public List<AppointmentDetailsDTO> getAppointmentsByPhysicianWithPatients(String physicianId) {
        List<Appointment> appointments = appointmentRepository.findByPhysicianPhysicianId(physicianId);

        return appointments.stream()
                .map(appointment -> {
                    try {
                        Map<String, Object> patientData = externalServiceClient.getPatientById(appointment.getPatientId());
                        appointment.setPatientName((String) patientData.get("fullName"));
                        appointment.setPatientEmail((String) patientData.get("email"));
                        appointment.setPatientPhone((String) patientData.get("phoneNumber"));
                        return new AppointmentDetailsDTO(appointment);
                    } catch (PatientNotFoundException e) {
                        return new AppointmentDetailsDTO(appointment);
                    }
                })
                .toList();
    }

    private Appointment mapRemoteAppointment(Map<String, Object> m) {
        String appointmentId = (String) m.get("appointmentId");
        String patientId = (String) m.get("patientId");
        String physicianId = (String) m.get("physicianId");
        String dateTimeStr = String.valueOf(m.get("dateTime"));
        String consultationTypeStr = String.valueOf(m.get("consultationType"));
        String statusStr = String.valueOf(m.get("status"));

        LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr);
        ConsultationType consultationType = ConsultationType.valueOf(consultationTypeStr);
        AppointmentStatus status = AppointmentStatus.valueOf(statusStr);

        Physician physician = physicianRepository.findById(physicianId).orElse(null);

        Appointment.AppointmentBuilder builder = Appointment.builder()
                .appointmentId(appointmentId)
                .patientId(patientId)
                .physician(physician)
                .dateTime(dateTime)
                .consultationType(consultationType)
                .status(status)
                .wasRescheduled(false);

        try {
            Map<String, Object> patientData = externalServiceClient.getPatientById(patientId);
            builder.patientName((String) patientData.get("fullName"));
            builder.patientEmail((String) patientData.get("email"));
            builder.patientPhone((String) patientData.get("phoneNumber"));
        } catch (Exception e) {
        }

        return builder.build();
    }
}
