package leti_sisdis_6.hapappointmentrecords.api;

import leti_sisdis_6.hapappointmentrecords.*;
import leti_sisdis_6.hapappointmentrecords.dto.output.AppointmentListDTO;
import leti_sisdis_6.hapappointmentrecords.usermanagement.model.*;
import leti_sisdis_6.hapappointmentrecords.patient.repository.PatientRepository;
//import com.pcm.psoft.pcmclinic_api.auth.api.AuthHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import leti_sisdis_6.hapappointmentrecords.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointment", description = "Appointment management endpoints")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AuthHelper authHelper;
    private final PatientRepository patientRepository;

    @GetMapping("/{appointmentId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'PATIENT')")
    @Operation(
            summary = "Get appointment details by ID",
            description = "Retrieve appointment details by appointment ID",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Appointment details retrieved"),
                    @ApiResponse(responseCode = "404", description = "Appointment not found"),
                    @ApiResponse(responseCode = "403", description = "Access forbidden")
            }
    )
    public ResponseEntity<?> getAppointmentDetails(@PathVariable String appointmentId) {
        User currentUser = authHelper.getCurrentUser();
        Object dto = appointmentService.getAppointmentById(appointmentId, currentUser);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('PATIENT')")
    @Operation(
            summary = "View my appointments",
            description = "Retrieve all appointments for the authenticated patient",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of appointments retrieved successfully"),
                    @ApiResponse(responseCode = "403", description = "Access forbidden")
            }
    )
    public ResponseEntity<List<AppointmentListDTO>> getMyAppointments() {
        User currentUser = authHelper.getCurrentUser();
        List<AppointmentListDTO> appointments = appointmentService.listAppointmentsByPatient(currentUser.getId());
        return ResponseEntity.ok(appointments);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'PATIENT')")
    @Operation(
        summary = "Update an appointment",
        description = "Update an existing appointment's details",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Appointment updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Appointment not found"),
            @ApiResponse(responseCode = "403", description = "Access forbidden")
        }
    )
    public ResponseEntity<?> updateAppointment(
            @PathVariable String id,
            @RequestBody @Valid UpdateAppointmentRequest request) {
        User currentUser = authHelper.getCurrentUser();
        Object appointment = appointmentService.getAppointmentById(id, currentUser);

        // Verify that the patient owns the appointment or user is admin
        if (currentUser.getRole() == Role.PATIENT &&
            appointment instanceof AppointmentDetailsDTO &&
            !currentUser.getId().equals(((AppointmentDetailsDTO) appointment).getPatientId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Object updatedAppointment = appointmentService.updateAppointment(id, request, currentUser);
        return ResponseEntity.ok(updatedAppointment);
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'PATIENT')")
    @Operation(
        summary = "Cancel an appointment",
        description = "Cancel an existing appointment",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Appointment cancelled successfully"),
            @ApiResponse(responseCode = "404", description = "Appointment not found"),
            @ApiResponse(responseCode = "403", description = "Access forbidden")
        }
    )
    public ResponseEntity<?> cancelAppointment(@PathVariable String id) {
        User currentUser = authHelper.getCurrentUser();
        Object appointment = appointmentService.getAppointmentById(id, currentUser);

        // Verify that the patient owns the appointment or user is admin
        if (currentUser.getRole() == Role.PATIENT &&
            appointment instanceof AppointmentDetailsDTO &&
            !currentUser.getId().equals(((AppointmentDetailsDTO) appointment).getPatientId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Object cancelledAppointment = appointmentService.cancelAppointment(id, currentUser);
        return ResponseEntity.ok(cancelledAppointment);
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

    @GetMapping("/average-duration")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
        summary = "Get average appointment duration per physician",
        description = "Calculates and returns the average duration of appointments for each physician",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved average durations"),
            @ApiResponse(responseCode = "403", description = "Access forbidden")
        }
    )
    public ResponseEntity<List<PhysicianAverageDTO>> getAverageDurationsPerPhysician() {
        List<PhysicianAverageDTO> averages = appointmentService.getAverageDurationsPerPhysician();
        return ResponseEntity.ok(averages);
    }

    @GetMapping("/physicians/{id}/slots")
    @PreAuthorize("hasAuthority('PATIENT')")
    @Operation(
        summary = "Get available appointment slots",
        description = "Retrieve available time slots for scheduling appointments with a specific physician",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Available slots retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Access forbidden"),
            @ApiResponse(responseCode = "404", description = "Physician not found")
        }
    )
    public ResponseEntity<List<AppointmentSlotDto>> getAvailableSlots(
            @PathVariable("id") String physicianId,
            @RequestParam("start") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate) {
        List<AppointmentSlotDto> slots = appointmentService.getAvailableSlots(physicianId, startDate);
        return ResponseEntity.ok(slots);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
        summary = "Schedule an appointment",
        description = "Schedule a new appointment between a patient and a physician",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "201", description = "Appointment scheduled successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Access forbidden"),
            @ApiResponse(responseCode = "404", description = "Patient or physician not found"),
            @ApiResponse(responseCode = "409", description = "Time slot already taken")
        }
    )
    public ResponseEntity<AppointmentIdResponse> scheduleAppointment(
            @Valid @RequestBody ScheduleAppointmentRequest request) {
        return ResponseEntity.status(201)
                .body(appointmentService.scheduleAppointment(request));
    }
}
