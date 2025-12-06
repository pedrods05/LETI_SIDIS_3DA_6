package leti_sisdis_6.happhysicians.api;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import com.fasterxml.jackson.databind.ObjectMapper;
import leti_sisdis_6.happhysicians.dto.output.AppointmentDetailsDTO;
import leti_sisdis_6.happhysicians.dto.output.AppointmentListDTO;
import leti_sisdis_6.happhysicians.dto.output.AuditTrailDTO;
import leti_sisdis_6.happhysicians.eventsourcing.EventStore;
import leti_sisdis_6.happhysicians.eventsourcing.EventStoreService;
import leti_sisdis_6.happhysicians.model.Appointment;
import leti_sisdis_6.happhysicians.services.AppointmentService;
import leti_sisdis_6.happhysicians.services.ExternalServiceClient;
import leti_sisdis_6.happhysicians.command.AppointmentCommandService;
import leti_sisdis_6.happhysicians.query.AppointmentQueryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.client.RestTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import org.slf4j.MDC;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static leti_sisdis_6.happhysicians.config.RabbitMQConfig.CORRELATION_ID_HEADER;

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

    @Autowired
    private AppointmentCommandService appointmentCommandService;

    @Autowired
    private AppointmentQueryService appointmentQueryService;
    
    @Autowired
    private EventStoreService eventStoreService;
    
    @Autowired
    private ObjectMapper objectMapper;

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
        // Use Query Service (reads from MongoDB read model with fallback to write model)
        Optional<Appointment> appointment = appointmentQueryService.getAppointmentById(appointmentId);
        if (appointment.isPresent()) {
            return ResponseEntity.ok(appointment.get());
        }

        // Fallback to peer forwarding if not found
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
    public ResponseEntity<?> createAppointment(
            @RequestHeader(value = CORRELATION_ID_HEADER, required = false) String incomingCorrelationId,
            @RequestBody leti_sisdis_6.happhysicians.dto.input.ScheduleAppointmentRequest appointmentDTO) {
        // Capture correlation ID from header or generate new one
        String correlationId = (incomingCorrelationId == null || incomingCorrelationId.isBlank())
                ? UUID.randomUUID().toString()
                : incomingCorrelationId;
        MDC.put(CORRELATION_ID_HEADER, correlationId);
        
        try {
            // Use Command Service (writes to write model and publishes event)
            Appointment createdAppointment = appointmentCommandService.createAppointment(appointmentDTO, correlationId);
            return ResponseEntity.ok(createdAppointment);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } finally {
            MDC.remove(CORRELATION_ID_HEADER);
        }
    }

    @GetMapping
    @Operation(summary = "Get all appointments")
    public List<Appointment> getAllAppointments() {
        // Use Query Service (reads from MongoDB read model)
        return appointmentQueryService.getAllAppointments();
    }

    @GetMapping("/physician/{physicianId}")
    @Operation(summary = "Get appointments by physician")
    public List<Appointment> getAppointmentsByPhysician(@PathVariable String physicianId) {
        // Use Query Service (reads from MongoDB read model with fallback to write model)
        return appointmentQueryService.getAppointmentsByPhysician(physicianId);
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get appointments by patient")
    public List<Appointment> getAppointmentsByPatient(@PathVariable String patientId) {
        // Use Query Service (reads from MongoDB read model with fallback to write model)
        return appointmentQueryService.getAppointmentsByPatient(patientId);
    }

    @PutMapping("/{appointmentId}")
    @Operation(summary = "Update appointment by ID")
    public ResponseEntity<?> updateAppointment(
            @RequestHeader(value = CORRELATION_ID_HEADER, required = false) String incomingCorrelationId,
            @PathVariable String appointmentId, 
            @RequestBody leti_sisdis_6.happhysicians.dto.input.UpdateAppointmentRequest updateDTO) {
        // Capture correlation ID from header or generate new one
        String correlationId = (incomingCorrelationId == null || incomingCorrelationId.isBlank())
                ? UUID.randomUUID().toString()
                : incomingCorrelationId;
        MDC.put(CORRELATION_ID_HEADER, correlationId);
        
        try {
            // Use Command Service (writes to write model and publishes event)
            Appointment updatedAppointment = appointmentCommandService.updateAppointment(appointmentId, updateDTO, correlationId);
            if (updatedAppointment != null) {
                return ResponseEntity.ok(updatedAppointment);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } finally {
            MDC.remove(CORRELATION_ID_HEADER);
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
        // Use Query Service (reads from MongoDB read model)
        List<AppointmentListDTO> appointments = appointmentQueryService.listUpcomingAppointments();
        return ResponseEntity.ok(appointments);
    }

    @PutMapping("/{appointmentId}/cancel")
    @Operation(summary = "Cancel appointment by ID")
    public ResponseEntity<?> cancelAppointment(
            @RequestHeader(value = CORRELATION_ID_HEADER, required = false) String incomingCorrelationId,
            @PathVariable String appointmentId) {
        // Capture correlation ID from header or generate new one
        String correlationId = (incomingCorrelationId == null || incomingCorrelationId.isBlank())
                ? UUID.randomUUID().toString()
                : incomingCorrelationId;
        MDC.put(CORRELATION_ID_HEADER, correlationId);
        
        try {
            // Use Command Service (writes to write model and publishes event)
            Appointment updated = appointmentCommandService.cancelAppointment(appointmentId, correlationId);
            if (updated != null) {
                return ResponseEntity.ok(updated);
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Appointment not found"));
        } finally {
            MDC.remove(CORRELATION_ID_HEADER);
        }
    }

    @GetMapping("/{appointmentId}/audit-trail")
    @Operation(
            summary = "Get appointment audit trail",
            description = "Retrieves the complete event history (audit trail) for an appointment using Event Sourcing. Returns all events: ConsultationScheduled, NoteAdded, ConsultationCompleted, etc."
    )
    public ResponseEntity<List<AuditTrailDTO>> getAuditTrail(@PathVariable String appointmentId) {
        try {
            List<EventStore> events = eventStoreService.getEventHistory(appointmentId);
            List<AuditTrailDTO> auditTrail = events.stream()
                    .map(event -> {
                        try {
                            // Parse JSON strings to objects for better readability
                            Object eventDataObj = event.getEventData() != null 
                                    ? objectMapper.readValue(event.getEventData(), Object.class)
                                    : null;
                            Object metadataObj = event.getMetadata() != null 
                                    ? objectMapper.readValue(event.getMetadata(), Object.class)
                                    : null;
                            
                            return AuditTrailDTO.builder()
                                    .eventId(event.getEventId())
                                    .aggregateId(event.getAggregateId())
                                    .eventType(event.getEventType())
                                    .timestamp(event.getTimestamp())
                                    .eventData(eventDataObj)
                                    .aggregateVersion(event.getAggregateVersion())
                                    .correlationId(event.getCorrelationId())
                                    .userId(event.getUserId())
                                    .metadata(metadataObj)
                                    .build();
                        } catch (Exception e) {
                            // If parsing fails, return as string
                            return AuditTrailDTO.builder()
                                    .eventId(event.getEventId())
                                    .aggregateId(event.getAggregateId())
                                    .eventType(event.getEventType())
                                    .timestamp(event.getTimestamp())
                                    .eventData(event.getEventData())
                                    .aggregateVersion(event.getAggregateVersion())
                                    .correlationId(event.getCorrelationId())
                                    .userId(event.getUserId())
                                    .metadata(event.getMetadata())
                                    .build();
                        }
                    })
                    .toList();
            return ResponseEntity.ok(auditTrail);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{appointmentId}/notes")
    @Operation(
            summary = "Add note to appointment",
            description = "Adds a note to an appointment and generates a NoteAdded event in the Event Store"
    )
    public ResponseEntity<?> addNoteToAppointment(
            @RequestHeader(value = CORRELATION_ID_HEADER, required = false) String incomingCorrelationId,
            @PathVariable String appointmentId,
            @RequestBody Map<String, String> request) {
        // Capture correlation ID from header or generate new one
        String correlationId = (incomingCorrelationId == null || incomingCorrelationId.isBlank())
                ? UUID.randomUUID().toString()
                : incomingCorrelationId;
        MDC.put(CORRELATION_ID_HEADER, correlationId);
        
        try {
            String note = request.get("note");
            String userId = request.get("userId"); // Opcional
            
            if (note == null || note.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Note is required"));
            }
            
            appointmentCommandService.addNoteToAppointment(appointmentId, note, userId);
            return ResponseEntity.ok(Map.of("message", "Note added successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        } finally {
            MDC.remove(CORRELATION_ID_HEADER);
        }
    }

}
