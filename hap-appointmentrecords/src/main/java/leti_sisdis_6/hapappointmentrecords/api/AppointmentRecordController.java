package leti_sisdis_6.hapappointmentrecords.api;

import leti_sisdis_6.hapappointmentrecords.dto.input.AppointmentRecordRequest;
import leti_sisdis_6.hapappointmentrecords.dto.output.AppointmentRecordResponse;
import leti_sisdis_6.hapappointmentrecords.dto.output.AppointmentRecordViewDTO;
import leti_sisdis_6.hapappointmentrecords.dto.local.UserDTO;
import leti_sisdis_6.hapappointmentrecords.service.AppointmentRecordService;
import leti_sisdis_6.hapappointmentrecords.exceptions.NotFoundException;
import leti_sisdis_6.hapappointmentrecords.exceptions.UnauthorizedException;
import leti_sisdis_6.hapappointmentrecords.http.ExternalServiceClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appointment-records")
@RequiredArgsConstructor
@Tag(name = "Appointment Records", description = "Appointment record management endpoints")
public class AppointmentRecordController {
    private final AppointmentRecordService recordService;
    private final ExternalServiceClient externalServiceClient;
    private final RestTemplate restTemplate;

    @PostMapping("/{appointmentId}/record")
    // MUDANÇA: hasRole procura por 'ROLE_PHYSICIAN'
    @PreAuthorize("hasRole('PHYSICIAN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Record appointment details", description = "Record diagnosis, treatment recommendations, and prescriptions for a consultation.")
    @ApiResponse(responseCode = "201", description = "Record created successfully")
    @ApiResponse(responseCode = "403", description = "Access denied. Only physicians can record appointment details")
    @ApiResponse(responseCode = "404", description = "Appointment not found")
    @ApiResponse(responseCode = "500", description = "External service communication error")
    public ResponseEntity<?> recordAppointmentDetails(
            @PathVariable String appointmentId,
            @Valid @RequestBody AppointmentRecordRequest request,
            @RequestHeader("X-User-Id") String physicianId) {
        try {
            AppointmentRecordResponse response = recordService.createRecord(appointmentId, request, physicianId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "External service communication error: " + e.getMessage()));
        }
    }

    @GetMapping("/{recordId}")
    // MUDANÇA: hasAnyRole procura por 'ROLE_ADMIN' ou 'ROLE_PATIENT'
    @PreAuthorize("hasAnyRole('ADMIN', 'PATIENT')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "View appointment record", description = "View appointment record details by record ID.")
    @ApiResponse(responseCode = "200", description = "Record found successfully")
    @ApiResponse(responseCode = "403", description = "Access denied. You are not authorized to view this record")
    @ApiResponse(responseCode = "404", description = "Record not found")
    @ApiResponse(responseCode = "500", description = "External service communication error")
    public ResponseEntity<?> viewAppointmentRecord(
            @PathVariable String recordId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole,
            HttpServletRequest request) {
        try {
            UserDTO currentUser = UserDTO.builder().id(userId).role(userRole).build();
            AppointmentRecordViewDTO record = recordService.getAppointmentRecord(recordId, currentUser);
            return ResponseEntity.ok(record);
        } catch (NotFoundException e) {
            if (request.getHeader("X-Peer-Request") != null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
            }
            // Peer forwarding logic...
            HttpHeaders fwd = new HttpHeaders();
            String auth = request.getHeader("Authorization");
            if (auth != null && !auth.isBlank()) fwd.add("Authorization", auth);
            fwd.add("X-User-Id", userId);
            fwd.add("X-User-Role", userRole);
            fwd.add("X-Peer-Request", "true");
            HttpEntity<Void> entity = new HttpEntity<>(fwd);

            for (String peer : externalServiceClient.getPeerUrls()) {
                String url = peer + "/api/appointment-records/" + recordId;
                try {
                    var resp = restTemplate.exchange(url, HttpMethod.GET, entity, AppointmentRecordViewDTO.class);
                    if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                        return ResponseEntity.ok(resp.getBody());
                    }
                } catch (Exception ignored) { }
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "External service communication error: " + e.getMessage()));
        }
    }

    @GetMapping("/patient/mine")
    // MUDANÇA: hasRole procura por 'ROLE_PATIENT'
    @PreAuthorize("hasRole('PATIENT')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "View my appointment records", description = "Patients can view all their appointment records.")
    public ResponseEntity<?> getMyRecords(@RequestHeader("X-User-Id") String patientId) {
        try {
            List<AppointmentRecordViewDTO> records = recordService.getPatientRecords(patientId);
            return ResponseEntity.ok(records);
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "External service communication error: " + e.getMessage()));
        }
    }

    @GetMapping("/patient/{patientId}")
    // MUDANÇA: hasRole procura por 'ROLE_PHYSICIAN'
    @PreAuthorize("hasRole('PHYSICIAN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "View patient's appointment records", description = "Physicians can view all appointment records of a specific patient.")
    public ResponseEntity<?> getPatientRecords(
            @PathVariable String patientId,
            @RequestHeader("X-User-Role") String userRole) {
        try {
            if (!"PHYSICIAN".equals(userRole)) {
                throw new UnauthorizedException("Only physicians can view patient records");
            }
            List<AppointmentRecordViewDTO> records = recordService.getPatientRecords(patientId);
            return ResponseEntity.ok(records);
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "External service communication error: " + e.getMessage()));
        }
    }
}