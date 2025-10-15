package leti_sisdis_6.hapappointmentrecords.api;

import leti_sisdis_6.hapappointmentrecords.dto.input.ScheduleAppointmentRequest;
import leti_sisdis_6.hapappointmentrecords.dto.output.AppointmentIdResponse;
import leti_sisdis_6.hapappointmentrecords.dto.output.AppointmentHistoryDTO;
import leti_sisdis_6.hapappointmentrecords.service.AppointmentService;
import leti_sisdis_6.happatients.model.Patient;
import leti_sisdis_6.happatients.repository.PatientRepository;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/patients/appointments")
@RequiredArgsConstructor
@Tag(name = "Patient Appointment", description = "Patient appointment management endpoints")
public class PatientAppointmentController {

    private final AppointmentService appointmentService;
    private final PatientRepository patientRepository;

    @PostMapping
    @PreAuthorize("hasAuthority('PATIENT')")
    @Operation(
        summary = "Schedule an appointment",
        description = "Schedule a new appointment with a physician",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "201", description = "Appointment scheduled successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Patient or physician not found"),
            @ApiResponse(responseCode = "409", description = "Time slot already taken")
        }
    )
    public ResponseEntity<AppointmentIdResponse> scheduleAppointment(
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.oauth2.jwt.Jwt principal,
            @Valid @RequestBody ScheduleAppointmentRequest request) {
        String patientEmail = principal.getSubject();
        Patient patient = patientRepository.findByEmail(patientEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
        
        if (!request.getPatientId().equals(patient.getPatientId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Patient ID does not match authenticated user");
        }
        
        return ResponseEntity.status(201)
                .body(appointmentService.scheduleAppointment(patientEmail, request));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAuthority('PATIENT')")
    @Operation(
        summary = "Get appointment history",
        description = "Retrieve the history of completed appointments with their medical records",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Appointment history retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Access forbidden")
        }
    )
    public ResponseEntity<List<AppointmentHistoryDTO>> getAppointmentHistory(
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.oauth2.jwt.Jwt principal) {
        String patientEmail = principal.getSubject();
        Patient patient = patientRepository.findByEmail(patientEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
        
        List<AppointmentHistoryDTO> history = appointmentService.getCompletedAppointmentsWithDetails(patient.getPatientId());
        return ResponseEntity.ok(history);
    }
} 