package leti_sisdis_6.hapappointmentrecords.api;

import leti_sisdis_6.hapappointmentrecords.dto.input.AppointmentRecordRequest;
import leti_sisdis_6.hapappointmentrecords.dto.output.AppointmentRecordResponse;
import leti_sisdis_6.hapappointmentrecords.dto.output.AppointmentRecordViewDTO;
import leti_sisdis_6.hapappointmentrecords.dto.local.UserDTO;
import leti_sisdis_6.hapappointmentrecords.service.AppointmentRecordService;
import leti_sisdis_6.hapappointmentrecords.exceptions.NotFoundException;
import leti_sisdis_6.hapappointmentrecords.exceptions.UnauthorizedException;
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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appointment-records")
@RequiredArgsConstructor
@Tag(name = "Appointment Records", description = "Appointment record management endpoints")
public class AppointmentRecordController {
    private final AppointmentRecordService recordService;

    @PostMapping("/{appointmentId}/record")
    @PreAuthorize("hasAuthority('PHYSICIAN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Record appointment details", description = "Record diagnosis, treatment recommendations, and prescriptions for a consultation. This endpoint demonstrates HTTP communication with hap-physicians service.")
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
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "External service communication error: " + e.getMessage()));
        }
    }

    @GetMapping("/{recordId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'PATIENT')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "View appointment record", description = "View appointment record details by record ID. This endpoint demonstrates HTTP communication with hap-physicians and hap-patients services.")
    @ApiResponse(responseCode = "200", description = "Record found successfully")
    @ApiResponse(responseCode = "403", description = "Access denied. You are not authorized to view this record")
    @ApiResponse(responseCode = "404", description = "Record not found")
    @ApiResponse(responseCode = "500", description = "External service communication error")
    public ResponseEntity<?> viewAppointmentRecord(
            @PathVariable String recordId,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String userRole) {
        try {
            UserDTO currentUser = UserDTO.builder()
                    .id(userId)
                    .role(userRole)
                    .build();
            AppointmentRecordViewDTO record = recordService.getAppointmentRecord(recordId, currentUser);
            return ResponseEntity.ok(record);
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "External service communication error: " + e.getMessage()));
        }
    }

    @GetMapping("/patient/mine")
    @PreAuthorize("hasAuthority('PATIENT')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "View my appointment records", description = "Patients can view all their appointment records. This endpoint demonstrates HTTP communication with hap-physicians service.")
    @ApiResponse(responseCode = "200", description = "Records retrieved successfully")
    @ApiResponse(responseCode = "403", description = "Access denied. Only patients can view their records")
    @ApiResponse(responseCode = "500", description = "External service communication error")
    public ResponseEntity<?> getMyRecords(@RequestHeader("X-User-Id") String patientId) {
        try {
            List<AppointmentRecordViewDTO> records = recordService.getPatientRecords(patientId);
            return ResponseEntity.ok(records);
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "External service communication error: " + e.getMessage()));
        }
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAuthority('PHYSICIAN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "View patient's appointment records", description = "Physicians can view all appointment records of a specific patient. This endpoint demonstrates HTTP communication with hap-physicians service.")
    @ApiResponse(responseCode = "200", description = "Records retrieved successfully")
    @ApiResponse(responseCode = "403", description = "Access denied. Only physicians can view patient records")
    @ApiResponse(responseCode = "404", description = "Patient not found")
    @ApiResponse(responseCode = "500", description = "External service communication error")
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
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "External service communication error: " + e.getMessage()));
        }
    }
}