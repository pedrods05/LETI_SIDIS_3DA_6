package leti_sisdis_6.happatients.api;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import leti_sisdis_6.happatients.dto.PatientDetailsDTO;
import leti_sisdis_6.happatients.dto.ContactDetailsUpdateDTO;
import leti_sisdis_6.happatients.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import leti_sisdis_6.happatients.dto.PatientProfileDTO;
import leti_sisdis_6.happatients.http.ResilientRestTemplate;
import leti_sisdis_6.happatients.repository.PatientLocalRepository;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
@Tag(name = "Patient", description = "Patient management endpoints")
public class PatientController {
    private final PatientService patientService;

    // Local in-memory cache following the requested structure
    private final PatientLocalRepository localRepo;

    // Fallback peers and resilient HTTP client
    private final ResilientRestTemplate resilientRestTemplate;

    // Hardcoded patient peers; we'll remove self based on server.port, but allow property override
    private List<String> peers = new ArrayList<>(Arrays.asList(
            "http://localhost:8082",
            "http://localhost:8088"
    ));

    @Value("${server.port:0}")
    private int serverPort;

    @Value("${hap.patients.peers:}")
    private String patientPeersProp;

    @PostConstruct
    void initPeers() {
        if (patientPeersProp != null && !patientPeersProp.isBlank()) {
            peers = parsePeers(patientPeersProp);
        }
        if (serverPort > 0) {
            peers.remove("http://localhost:" + serverPort);
        }
    }

    private List<String> parsePeers(String prop) {
        List<String> list = new ArrayList<>();
        for (String s : prop.split(",")) {
            String v = s.trim();
            if (!v.isEmpty()) list.add(v);
        }
        return list;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
        summary = "List all patients",
        description = "Retrieves all patients (admin only)",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved patient list"),
            @ApiResponse(responseCode = "403", description = "Access forbidden")
        }
    )
    public ResponseEntity<List<PatientDetailsDTO>> listAllPatients() {
        return ResponseEntity.ok(patientService.listAllPatients());
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get patient details",
        description = "Retrieves patient details. Only accessible by administrators.",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved patient details"),
            @ApiResponse(responseCode = "404", description = "Patient not found"),
            @ApiResponse(responseCode = "403", description = "Access forbidden")
        }
    )
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> getPatientDetails(
            @PathVariable String id,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
        // 1) Try local cache first
        PatientDetailsDTO cached = localRepo.findById(id).orElse(null);
        if (cached != null) {
            return ResponseEntity.ok(cached);
        }
        // 2) Try local service (DB)
        try {
            PatientDetailsDTO patient = patientService.getPatientDetails(id);
            // store in local cache for future quick hits
            localRepo.save(patient);
            return ResponseEntity.ok(patient);
        } catch (EntityNotFoundException e) {
            // 3) Fallback to peers hitting INTERNAL endpoint (no auth required)
            for (String peer : peers) {
                String url = (peer.endsWith("/")) ? (peer + "internal/patients/" + id) : (peer + "/internal/patients/" + id);
                try {
                    PatientDetailsDTO remote = resilientRestTemplate.getForObjectWithFallback(url, PatientDetailsDTO.class);
                    if (remote != null) {
                        localRepo.save(remote);
                        return ResponseEntity.ok(remote);
                    }
                } catch (Exception ignored) {
                    // ignore and try next peer
                }
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Patient not found"));
        }
    }

    // Alias to match required path: /patients/search
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
        summary = "Search patients by name",
        description = "Allows administrators to search for patients using part of the full name.",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved patient list"),
            @ApiResponse(responseCode = "404", description = "No patients found"),
            @ApiResponse(responseCode = "403", description = "Access forbidden")
        }
    )
    public ResponseEntity<?> searchPatients(@RequestParam String name) {
        try {
            List<PatientDetailsDTO> result = patientService.searchPatientsByName(name);
            return ResponseEntity.ok(result);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/profile")
    @PreAuthorize("hasAuthority('PHYSICIAN')")
    @Operation(
        summary = "Get patient profile",
        description = "Returns a patient's profile enriched with appointment records. Accessible to physicians.",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Patient not found"),
            @ApiResponse(responseCode = "403", description = "Access forbidden")
        }
    )
    public ResponseEntity<?> getPatientProfile(
            @PathVariable String id,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
        try {
            PatientProfileDTO profile = patientService.getPatientProfile(id, authorizationHeader);
            return ResponseEntity.ok(profile);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/me")
    @PreAuthorize("hasAuthority('PATIENT')")
    @Operation(
        summary = "Update patient contact details",
        description = "Updates the contact details of the authenticated patient. Only phoneNumber, address, and photo can be updated.",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Contact details updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or unknown fields provided"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
        }
    )
    public ResponseEntity<?> updateContactDetails(@Valid @RequestBody ContactDetailsUpdateDTO dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        String message = patientService.updateContactDetails(email, dto);
        return ResponseEntity.ok(new UpdateResponse(message));
    }

    @ExceptionHandler(UnrecognizedPropertyException.class)
    public ResponseEntity<ErrorResponse> handleUnrecognizedPropertyException(UnrecognizedPropertyException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
            "Invalid request",
            "Unknown field: " + e.getPropertyName() + ". Only phoneNumber, address, and photo can be updated.",
            "BAD_REQUEST"
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
            "Invalid request",
            e.getMessage(),
            "BAD_REQUEST"
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
            "Invalid request",
            e.getMessage(),
            "BAD_REQUEST"
        ));
    }

    private record UpdateResponse(String message) {}
    private record ErrorResponse(String error, String message, String status) {}
}
