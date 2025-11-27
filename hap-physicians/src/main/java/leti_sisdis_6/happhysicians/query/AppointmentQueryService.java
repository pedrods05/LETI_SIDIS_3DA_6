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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
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
        List<AppointmentSummary> summaries = appointmentQueryRepository
                .findByDateTimeAfterOrderByDateTimeAsc(LocalDateTime.now())
                .stream()
                .filter(s -> "SCHEDULED".equals(s.getStatus()))
                .collect(Collectors.toList());

        List<Appointment> appointments = summaries.stream()
                .map(this::toAppointmentWithFallback)
                .collect(Collectors.toList());

        return appointmentMapper.toListDTO(appointments);
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
                        System.out.println("✅ Enriched patient data for appointment: " + appointment.getAppointmentId());
                    }
                } catch (Exception e) {
                    System.out.println("⚠️ Warning: Could not enrich patient data for appointment " + appointment.getAppointmentId() + ": " + e.getMessage());
                }
            }
            return appointment;
        }

        // If not in write model, build from read model (fallback)
        Appointment.AppointmentBuilder builder = Appointment.builder()
                .appointmentId(summary.getId())
                .patientId(summary.getPatientId())
                .dateTime(summary.getDateTime())
                .consultationType(ConsultationType.valueOf(summary.getConsultationType()))
                .status(AppointmentStatus.valueOf(summary.getStatus()))
                .wasRescheduled(false);

        // Try to enrich with patient data
        try {
            Map<String, Object> patientData = externalServiceClient.getPatientById(summary.getPatientId());
            if (patientData != null) {
                builder.patientName((String) patientData.get("fullName"));
                builder.patientEmail((String) patientData.get("email"));
                builder.patientPhone((String) patientData.get("phoneNumber"));
            }
        } catch (Exception e) {
            System.out.println("⚠️ Warning: Could not fetch patient data: " + e.getMessage());
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

