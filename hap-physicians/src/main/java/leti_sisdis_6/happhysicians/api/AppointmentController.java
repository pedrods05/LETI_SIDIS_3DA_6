package leti_sisdis_6.happhysicians.api;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import leti_sisdis_6.happhysicians.dto.output.AppointmentDetailsDTO;
import leti_sisdis_6.happhysicians.dto.output.AppointmentListDTO;
import leti_sisdis_6.happhysicians.model.Appointment;
import leti_sisdis_6.happhysicians.services.AppointmentService;
import leti_sisdis_6.happhysicians.services.ExternalServiceClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.client.RestTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

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
    @PreAuthorize("hasAnyAuthority('ADMIN', 'PATIENT')")
    @Operation(
            summary = "Get appointment by ID",
    description = "Retrieve appointment details by appointment ID",
    security = @SecurityRequirement(name = "bearerAuth"),
    responses = {
        @ApiResponse(responseCode = "200", description = "Appointment details retrieved"),
        @ApiResponse(responseCode = "404", description = "Appointment not found"),
        @ApiResponse(responseCode = "403", description = "Access forbidden")
    }


    )
    public ResponseEntity<Appointment> getAppointment(@PathVariable String appointmentId) {
        Optional<Appointment> appointment = appointmentService.getAppointmentById(appointmentId);
        if (appointment.isPresent()) {
            return ResponseEntity.ok(appointment.get());
        }

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
            }
        }
        System.out.println("Appointment not found in any peer");
        return ResponseEntity.notFound().build();
    }

    @PostMapping
    @Operation(summary = "Create a new appointment")
    public ResponseEntity<?> createAppointment(@RequestBody leti_sisdis_6.happhysicians.dto.input.ScheduleAppointmentRequest appointmentDTO) {
        try {
            Appointment createdAppointment = appointmentService.createAppointment(appointmentDTO);
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
    public ResponseEntity<?> updateAppointment(@PathVariable String appointmentId, @RequestBody leti_sisdis_6.happhysicians.dto.input.UpdateAppointmentRequest updateDTO) {
        try {
            Appointment updatedAppointment = appointmentService.updateAppointment(appointmentId, updateDTO);
            if (updatedAppointment != null) {
                return ResponseEntity.ok(updatedAppointment);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
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
    @GetMapping("/upcoming")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
            summary = "List upcoming appointments",
            description = "Retrieves a list of all upcoming appointments sorted chronologically",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved upcoming appointments"),
                    @ApiResponse(responseCode = "403", description = "Access forbidden")
            }
    )
    public ResponseEntity<List<AppointmentListDTO>> listUpcomingAppointments() {
        List<AppointmentListDTO> appointments = appointmentService.listUpcomingAppointments();
        return ResponseEntity.ok(appointments);
    }

    @PutMapping("/{appointmentId}/cancel")
    @Operation(summary = "Cancel appointment by ID")
    public ResponseEntity<?> cancelAppointment(@PathVariable String appointmentId) {
        Appointment updated = appointmentService.cancelAppointment(appointmentId);
        if (updated != null) {
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Appointment not found"));
    }


}
