package leti_sisdis_6.happatients.api;

import leti_sisdis_6.happatients.dto.PatientRegistrationDTOV2;
import leti_sisdis_6.happatients.exceptions.EmailAlreadyExistsException;
import leti_sisdis_6.happatients.service.PatientRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static leti_sisdis_6.happatients.config.RabbitMQConfig.CORRELATION_ID_HEADER;

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
    public ResponseEntity<?> registerPatient(
            @RequestHeader(value = CORRELATION_ID_HEADER, required = false) String incomingCorrelationId,
            @Valid @RequestBody PatientRegistrationDTOV2 request) {

        String correlationId = (incomingCorrelationId == null || incomingCorrelationId.isBlank())
                ? UUID.randomUUID().toString()
                : incomingCorrelationId;
        MDC.put(CORRELATION_ID_HEADER, correlationId);

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
        } finally {
            MDC.remove(CORRELATION_ID_HEADER);
        }
    }

    private record RegistrationResponse(String patientId, String message) {}
    private record ErrorResponse(String error, String message, String status) {}
}
