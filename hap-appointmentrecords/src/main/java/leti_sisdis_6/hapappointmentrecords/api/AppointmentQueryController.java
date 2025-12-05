package leti_sisdis_6.hapappointmentrecords.api;

import leti_sisdis_6.hapappointmentrecords.http.ExternalServiceClient;
import leti_sisdis_6.hapappointmentrecords.model.Appointment;
import leti_sisdis_6.hapappointmentrecords.model.AppointmentProjection;
import leti_sisdis_6.hapappointmentrecords.repository.AppointmentProjectionRepository;
import leti_sisdis_6.hapappointmentrecords.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import leti_sisdis_6.hapappointmentrecords.model.AppointmentStatus;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentQueryController {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentProjectionRepository appointmentProjectionRepository;
    private final ExternalServiceClient externalServiceClient;
    private final RestTemplate restTemplate;

    @GetMapping
    public List<Appointment> listAll() {
        // Read from projections, map to Appointment DTO for compatibility
        return appointmentProjectionRepository.findAll().stream()
                .map(p -> Appointment.builder()
                        .appointmentId(p.getAppointmentId())
                        .patientId(p.getPatientId())
                        .physicianId(p.getPhysicianId())
                        .dateTime(p.getDateTime())
                        .consultationType(p.getConsultationType())
                        .status(p.getStatus())
                        .build())
                .toList();
    }

    @GetMapping("/{appointmentId}")
    public ResponseEntity<Appointment> getById(@PathVariable String appointmentId) {
        // Check local projection first
        ResponseEntity<Appointment> localResult = appointmentProjectionRepository.findById(appointmentId)
                .map(p -> Appointment.builder()
                        .appointmentId(p.getAppointmentId())
                        .patientId(p.getPatientId())
                        .physicianId(p.getPhysicianId())
                        .dateTime(p.getDateTime())
                        .consultationType(p.getConsultationType())
                        .status(p.getStatus())
                        .build())
                .map(ResponseEntity::ok)
                .orElse(null);

         if (localResult != null) {
             return localResult;
         }

         // Query peers if not found locally
         System.out.println("Appointment not found locally, querying peers");
         List<String> peers = externalServiceClient.getPeerUrls();
         for (String peer : peers) {
             String url = peer + "/internal/appointments/" + appointmentId;
             System.out.println("Querying peer: " + url);
             try {
                 Appointment remoteAppointment = restTemplate.getForObject(url, Appointment.class);
                 if (remoteAppointment != null) {
                     System.out.println("Found appointment in peer: " + url);
                     return ResponseEntity.ok(remoteAppointment);
                 }
             } catch (Exception e) {
                 System.out.println("Failed to query peer " + peer + ": " + e.getMessage());
                 // continue to next peer
             }
         }
         System.out.println("Appointment not found in any peer");
         return ResponseEntity.notFound().build();
    }

    @GetMapping("/patient/{patientId}")
    public List<Appointment> getByPatient(@PathVariable String patientId) {
        return appointmentProjectionRepository.findByPatientId(patientId).stream()
                .map(p -> Appointment.builder()
                        .appointmentId(p.getAppointmentId())
                        .patientId(p.getPatientId())
                        .physicianId(p.getPhysicianId())
                        .dateTime(p.getDateTime())
                        .consultationType(p.getConsultationType())
                        .status(p.getStatus())
                        .build())
                .toList();
    }

    @GetMapping("/physician/{physicianId}")
    public List<Appointment> getByPhysician(@PathVariable String physicianId) {
        return appointmentProjectionRepository.findByPhysicianId(physicianId).stream()
                .map(p -> Appointment.builder()
                        .appointmentId(p.getAppointmentId())
                        .patientId(p.getPatientId())
                        .physicianId(p.getPhysicianId())
                        .dateTime(p.getDateTime())
                        .consultationType(p.getConsultationType())
                        .status(p.getStatus())
                        .build())
                .toList();
    }

    @PostMapping
    public ResponseEntity<?> createAppointment(@RequestBody Appointment appointment) {
        try {
            // Check if appointment already exists
            if (appointmentProjectionRepository.existsById(appointment.getAppointmentId())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Appointment with ID " + appointment.getAppointmentId() + " already exists"));
            }

            // Check for time conflicts
            var conflictingAppointments = appointmentProjectionRepository
                    .findByPhysicianIdAndDateTime(appointment.getPhysicianId(), appointment.getDateTime());

            if (!conflictingAppointments.isEmpty()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Physician already has an appointment at this time"));
            }

            // Write model path keeps using JPA to persist the source-of-truth
            Appointment saved = appointmentRepository.save(appointment);

            // Upsert projection for read model
            AppointmentProjection projection = AppointmentProjection.builder()
                    .appointmentId(saved.getAppointmentId())
                    .patientId(saved.getPatientId())
                    .physicianId(saved.getPhysicianId())
                    .dateTime(saved.getDateTime())
                    .consultationType(saved.getConsultationType())
                    .status(saved.getStatus())
                    .lastUpdated(java.time.LocalDateTime.now())
                    .build();
            appointmentProjectionRepository.save(projection);

            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/internal/{appointmentId}")
    public ResponseEntity<?> updateAppointmentInternal(
            @PathVariable String appointmentId,
            @RequestBody Appointment appointmentDetails) {
        try {
            return appointmentRepository.findById(appointmentId)
                    .map(existing -> {
                        existing.setPatientId(appointmentDetails.getPatientId());
                        existing.setPhysicianId(appointmentDetails.getPhysicianId());
                        existing.setDateTime(appointmentDetails.getDateTime());
                        existing.setConsultationType(appointmentDetails.getConsultationType());
                        existing.setStatus(appointmentDetails.getStatus());
                        Appointment updated = appointmentRepository.save(existing);

                        // Upsert projection for read model
                        AppointmentProjection projection = AppointmentProjection.builder()
                                .appointmentId(updated.getAppointmentId())
                                .patientId(updated.getPatientId())
                                .physicianId(updated.getPhysicianId())
                                .dateTime(updated.getDateTime())
                                .consultationType(updated.getConsultationType())
                                .status(updated.getStatus())
                                .lastUpdated(java.time.LocalDateTime.now())
                                .build();
                        appointmentProjectionRepository.save(projection);

                        return ResponseEntity.ok(updated);
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/internal/{appointmentId}/cancel")
    public ResponseEntity<?> cancelAppointmentInternal(@PathVariable String appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .map(existingAppointment -> {
                    existingAppointment.setStatus(AppointmentStatus.CANCELLED);
                    Appointment updated = appointmentRepository.save(existingAppointment);

                    // Upsert projection for read model
                    AppointmentProjection projection = AppointmentProjection.builder()
                            .appointmentId(updated.getAppointmentId())
                            .patientId(updated.getPatientId())
                            .physicianId(updated.getPhysicianId())
                            .dateTime(updated.getDateTime())
                            .consultationType(updated.getConsultationType())
                            .status(updated.getStatus())
                            .lastUpdated(java.time.LocalDateTime.now())
                            .build();
                    appointmentProjectionRepository.save(projection);

                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Public endpoints expected by tests
    @PutMapping("/{appointmentId}")
    public ResponseEntity<?> updateAppointment(
            @PathVariable String appointmentId,
            @RequestBody Appointment appointmentDetails) {
        return updateAppointmentInternal(appointmentId, appointmentDetails);
    }

    @PutMapping("/{appointmentId}/cancel")
    public ResponseEntity<?> cancelAppointment(@PathVariable String appointmentId) {
        return cancelAppointmentInternal(appointmentId);
    }
}
