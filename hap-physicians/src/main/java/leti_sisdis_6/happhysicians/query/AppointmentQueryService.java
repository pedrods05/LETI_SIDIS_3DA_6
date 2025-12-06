package leti_sisdis_6.happhysicians.query;

import leti_sisdis_6.happhysicians.api.AppointmentMapper;
import leti_sisdis_6.happhysicians.dto.output.AppointmentListDTO;
import leti_sisdis_6.happhysicians.model.Appointment;
import leti_sisdis_6.happhysicians.model.AppointmentStatus;
import leti_sisdis_6.happhysicians.model.ConsultationType;
import leti_sisdis_6.happhysicians.model.Physician;
import leti_sisdis_6.happhysicians.repository.AppointmentRepository;
import leti_sisdis_6.happhysicians.repository.PhysicianRepository;
import leti_sisdis_6.happhysicians.services.ExternalServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentQueryService {

    private final AppointmentQueryRepository appointmentQueryRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentMapper appointmentMapper;
    private final PhysicianRepository physicianRepository;
    private final ExternalServiceClient externalServiceClient;

    public List<Appointment> getAllAppointments() {
        // Try read model first, fallback to write model
        List<AppointmentSummary> summaries = appointmentQueryRepository.findAll();
        if (!summaries.isEmpty()) {
            return summaries.stream()
                    .map(this::toAppointmentWithFallback)
                    .collect(Collectors.toList());
        }
        // Fallback to write model if read model is empty
        return appointmentRepository.findAll();
    }

    public Optional<Appointment> getAppointmentById(String appointmentId) {
        // Try read model first
        Optional<AppointmentSummary> summary = appointmentQueryRepository.findById(appointmentId);
        if (summary.isPresent()) {
            return Optional.of(toAppointmentWithFallback(summary.get()));
        }
        // Fallback to write model
        return appointmentRepository.findById(appointmentId);
    }

    public List<AppointmentListDTO> listUpcomingAppointments() {
        // Try read model first
        List<AppointmentSummary> summaries = appointmentQueryRepository
                .findByDateTimeAfterOrderByDateTimeAsc(LocalDateTime.now())
                .stream()
                .filter(s -> "SCHEDULED".equals(s.getStatus()))
                .collect(Collectors.toList());

        if (!summaries.isEmpty()) {
            List<Appointment> appointments = summaries.stream()
                    .map(this::toAppointmentWithFallback)
                    .collect(Collectors.toList());
            return appointmentMapper.toListDTO(appointments);
        }

        // Fallback to write model if read model is empty or has no upcoming appointments
        List<Appointment> allAppointments = appointmentRepository.findAll();
        List<Appointment> upcomingAppointments = allAppointments.stream()
                .filter(a -> a.getDateTime().isAfter(LocalDateTime.now()))
                .filter(a -> a.getStatus() == AppointmentStatus.SCHEDULED)
                .sorted((a1, a2) -> a1.getDateTime().compareTo(a2.getDateTime()))
                .collect(Collectors.toList());

        return appointmentMapper.toListDTO(upcomingAppointments);
    }

    public List<Appointment> getAppointmentsByPhysician(String physicianId) {
        // Try read model first
        List<AppointmentSummary> summaries = appointmentQueryRepository.findByPhysicianId(physicianId);
        
        if (!summaries.isEmpty()) {
            return summaries.stream()
                    .map(this::toAppointmentWithFallback)
                    .collect(Collectors.toList());
        }
        
        // Fallback to write model if read model is empty
        return appointmentRepository.findByPhysicianPhysicianId(physicianId);
    }

    public List<Appointment> getAppointmentsByPatient(String patientId) {
        // Try read model first
        List<AppointmentSummary> summaries = appointmentQueryRepository.findByPatientId(patientId);
        
        if (!summaries.isEmpty()) {
            return summaries.stream()
                    .map(this::toAppointmentWithFallback)
                    .collect(Collectors.toList());
        }
        
        // Fallback to write model if read model is empty
        return appointmentRepository.findByPatientId(patientId);
    }

    private Appointment toAppointmentWithFallback(AppointmentSummary summary) {
        // Always try to get full appointment from write model first (has all fields)
        Optional<Appointment> fullAppointment = appointmentRepository.findById(summary.getId());
        if (fullAppointment.isPresent()) {
            Appointment appointment = fullAppointment.get();
            // If patient data is missing, try to fetch it from patients service
            if (appointment.getPatientName() == null || appointment.getPatientEmail() == null || appointment.getPatientPhone() == null) {
                try {
                    Map<String, Object> patientData = externalServiceClient.getPatientById(appointment.getPatientId());
                    if (patientData != null) {
                        appointment.setPatientName((String) patientData.get("fullName"));
                        appointment.setPatientEmail((String) patientData.get("email"));
                        appointment.setPatientPhone((String) patientData.get("phoneNumber"));
                        // Save enriched data back to write model for future queries
                        appointmentRepository.save(appointment);
                        log.debug("✅ Enriched patient data for appointment: {}", appointment.getAppointmentId());
                    }
                } catch (Exception e) {
                    log.warn("⚠️ Warning: Could not enrich patient data for appointment {}: {}", appointment.getAppointmentId(), e.getMessage());
                }
            }
            return appointment;
        }

        // If not in write model, build from read model (fallback)
        Appointment.AppointmentBuilder builder = Appointment.builder()
                .appointmentId(summary.getId())
                .patientId(summary.getPatientId())
                .wasRescheduled(false);
        
        // Safely set dateTime (handle null)
        if (summary.getDateTime() != null) {
            builder.dateTime(summary.getDateTime());
        } else {
            log.warn("⚠️ Warning: dateTime is null for appointment {}, using current time", summary.getId());
            builder.dateTime(java.time.LocalDateTime.now());
        }
        
        // Safely convert consultationType (handle null)
        if (summary.getConsultationType() != null && !summary.getConsultationType().isEmpty()) {
            try {
                builder.consultationType(ConsultationType.valueOf(summary.getConsultationType()));
            } catch (IllegalArgumentException e) {
                log.warn("⚠️ Warning: Invalid consultationType '{}' for appointment {}, defaulting to FIRST_TIME", summary.getConsultationType(), summary.getId());
                builder.consultationType(ConsultationType.FIRST_TIME);
            }
        } else {
            log.warn("⚠️ Warning: consultationType is null for appointment {}, defaulting to FIRST_TIME", summary.getId());
            builder.consultationType(ConsultationType.FIRST_TIME);
        }
        
        // Safely convert status (handle null)
        if (summary.getStatus() != null && !summary.getStatus().isEmpty()) {
            try {
                builder.status(AppointmentStatus.valueOf(summary.getStatus()));
            } catch (IllegalArgumentException e) {
                log.warn("⚠️ Warning: Invalid status '{}' for appointment {}, defaulting to SCHEDULED", summary.getStatus(), summary.getId());
                builder.status(AppointmentStatus.SCHEDULED);
            }
        } else {
            log.warn("⚠️ Warning: status is null for appointment {}, defaulting to SCHEDULED", summary.getId());
            builder.status(AppointmentStatus.SCHEDULED);
        }

        // Try to enrich with patient data
        try {
            Map<String, Object> patientData = externalServiceClient.getPatientById(summary.getPatientId());
            if (patientData != null) {
                builder.patientName((String) patientData.get("fullName"));
                builder.patientEmail((String) patientData.get("email"));
                builder.patientPhone((String) patientData.get("phoneNumber"));
            }
        } catch (Exception e) {
            log.warn("⚠️ Warning: Could not fetch patient data: {}", e.getMessage());
        }

        // Load physician if available
        Optional<Physician> physician = physicianRepository.findById(summary.getPhysicianId());
        if (physician.isPresent()) {
            builder.physician(physician.get());
        } else {
            // Create a minimal physician object if not found
            Physician minimalPhysician = new Physician();
            minimalPhysician.setPhysicianId(summary.getPhysicianId());
            builder.physician(minimalPhysician);
        }

        return builder.build();
    }
}

