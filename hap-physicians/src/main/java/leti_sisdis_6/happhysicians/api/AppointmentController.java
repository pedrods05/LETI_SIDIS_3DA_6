package leti_sisdis_6.happhysicians.api;

import leti_sisdis_6.happhysicians.dto.output.AppointmentDetailsDTO;
import leti_sisdis_6.happhysicians.model.Appointment;
import leti_sisdis_6.happhysicians.services.AppointmentService;
import leti_sisdis_6.happhysicians.services.ExternalServiceClient;
import org.springframework.web.client.RestTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/appointments")
@Tag(name = "Appointment", description = "Appointment management endpoints")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private ExternalServiceClient externalServiceClient;

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/{appointmentId}")
    @Operation(summary = "Get appointment by ID")
    public ResponseEntity<Appointment> getAppointment(@PathVariable String appointmentId) {
        // Check local store first
        Optional<Appointment> appointment = appointmentService.getAppointmentById(appointmentId);
        if (appointment.isPresent()) {
            return ResponseEntity.ok(appointment.get());
        }

        // Query peers if not found locally
        List<String> peers = externalServiceClient.getPeerUrls();
        for (String peer : peers) {
            try {
                Appointment remoteAppointment = restTemplate.getForObject(
                    peer + "/internal/appointments/" + appointmentId, Appointment.class);
                if (remoteAppointment != null) {
                    return ResponseEntity.ok(remoteAppointment);
                }
            } catch (Exception e) {
                // Log error and continue to next peer
                System.out.println("Failed to query peer " + peer + ": " + e.getMessage());
            }
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping
    @Operation(summary = "Create a new appointment")
    public ResponseEntity<?> createAppointment(@RequestBody Appointment appointment) {
        try {
            Appointment createdAppointment = appointmentService.createAppointment(appointment);
            return ResponseEntity.ok(createdAppointment);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    @Operation(summary = "Get all appointments")
    public List<Appointment> getAllAppointments() {
        return appointmentService.getAllAppointments();
    }

    @GetMapping("/physician/{physicianId}")
    @Operation(summary = "Get appointments by physician")
    public List<Appointment> getAppointmentsByPhysician(@PathVariable String physicianId) {
        return appointmentService.getAppointmentsByPhysician(physicianId);
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get appointments by patient")
    public List<Appointment> getAppointmentsByPatient(@PathVariable String patientId) {
        return appointmentService.getAppointmentsByPatient(patientId);
    }

    @PutMapping("/{appointmentId}")
    @Operation(summary = "Update appointment by ID")
    public ResponseEntity<Appointment> updateAppointment(@PathVariable String appointmentId, @RequestBody Appointment appointmentDetails) {
        Appointment updatedAppointment = appointmentService.updateAppointment(appointmentId, appointmentDetails);
        if (updatedAppointment != null) {
            return ResponseEntity.ok(updatedAppointment);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{appointmentId}")
    @Operation(summary = "Delete appointment by ID")
    public ResponseEntity<Void> deleteAppointment(@PathVariable String appointmentId) {
        if (appointmentService.deleteAppointment(appointmentId)) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // ===== ENDPOINTS DE COMUNICAÇÃO INTER-MICROSERVIÇOS =====


    @GetMapping("/{appointmentId}/full-details")
    @Operation(summary = "Get appointment with full details")
    public ResponseEntity<AppointmentDetailsDTO> getAppointmentWithPatientAndRecord(@PathVariable String appointmentId) {
        try {
            AppointmentDetailsDTO details = appointmentService.getAppointmentWithPatientAndRecord(appointmentId);
            return ResponseEntity.ok(details);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/physician/{physicianId}/with-patients")
    @Operation(summary = "Get physician appointments with patients")
    public ResponseEntity<List<AppointmentDetailsDTO>> getAppointmentsByPhysicianWithPatients(@PathVariable String physicianId) {
        try {
            List<AppointmentDetailsDTO> appointments = appointmentService.getAppointmentsByPhysicianWithPatients(physicianId);
            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

}
