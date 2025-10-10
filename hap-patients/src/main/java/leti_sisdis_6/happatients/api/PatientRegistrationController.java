package leti_sisdis_6.happatients.api;

import leti_sisdis_6.happatients.dto.PatientRegistrationDTOV2;
import leti_sisdis_6.happatients.exceptions.EmailAlreadyExistsException;
import leti_sisdis_6.happatients.service.PatientRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/patients")
@RequiredArgsConstructor
@Tag(name = "Patient Registration V2", description = "Enhanced patient registration endpoints")
public class PatientRegistrationController {
    private final PatientRegistrationService patientRegistrationService;

    @PostMapping("/register")
    @Operation(
        summary = "Register new patient (V2)",
        description = "Creates a new patient account with enhanced validation and data collection",
        responses = {
            @ApiResponse(responseCode = "201", description = "Patient registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "409", description = "Email already exists")
        }
    )
    public ResponseEntity<?> registerPatient(@Valid @RequestBody PatientRegistrationDTOV2 request) {
        try {
            String patientId = patientRegistrationService.registerPatient(request);
            return ResponseEntity.created(null)
                    .body(new RegistrationResponse(patientId, "Patient registered successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(
                "Invalid request",
                e.getMessage(),
                "BAD_REQUEST"
            ));
        } catch (EmailAlreadyExistsException e) {
            return ResponseEntity.status(409).body(new ErrorResponse(
                "Email already exists",
                e.getMessage(),
                "CONFLICT"
            ));
        }
    }

    private record RegistrationResponse(String patientId, String message) {}
    private record ErrorResponse(String error, String message, String status) {}
} 