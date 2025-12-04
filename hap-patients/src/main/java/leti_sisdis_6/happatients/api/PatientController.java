package leti_sisdis_6.happatients.api;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import leti_sisdis_6.happatients.dto.ContactDetailsUpdateDTO;
import leti_sisdis_6.happatients.dto.PatientDetailsDTO;
import leti_sisdis_6.happatients.dto.PatientProfileDTO;
import leti_sisdis_6.happatients.exceptions.NotFoundException;
import leti_sisdis_6.happatients.http.ResilientRestTemplate;
import leti_sisdis_6.happatients.service.PatientQueryService;
import leti_sisdis_6.happatients.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

import java.util.*;

@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
@Tag(name = "Patient", description = "Patient management endpoints")
public class PatientController {

    private final PatientService patientService;
    private final PatientQueryService patientQueryService;
    private final ResilientRestTemplate resilientRestTemplate;

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
        System.out.println("Patient peers initialized for port " + serverPort + ": " + peers);
    }

    private List<String> parsePeers(String prop) {
        List<String> list = new ArrayList<>();
        for (String s : prop.split(",")) {
            String v = s.trim();
            if (!v.isEmpty()) list.add(v);
        }
        return list;
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
    public ResponseEntity<?> getPatientDetails(@PathVariable String id) {
        try {
            PatientProfileDTO profile = patientQueryService.getPatientProfile(id);
            return ResponseEntity.ok(profile);
        } catch (NotFoundException e) {
            System.out.println("⚠️ Paciente não encontrado no Mongo (CQRS). Tentando Fallback SQL/Peers...");
        }

        try {
            PatientDetailsDTO patient = patientService.getPatientDetails(id);
            return ResponseEntity.ok(patient);
        } catch (EntityNotFoundException e) {
            System.out.println("Patient not found locally (SQL), querying peers: " + peers);

            for (String peer : peers) {
                String url = (peer.endsWith("/")) ? (peer + "internal/patients/" + id) : (peer + "/internal/patients/" + id);
                System.out.println("Querying peer: " + url);
                try {
                    PatientDetailsDTO remote = resilientRestTemplate.getForObjectWithFallback(url, PatientDetailsDTO.class);
                    if (remote != null) {
                        System.out.println("Found patient in peer: " + url);
                        return ResponseEntity.ok(remote);
                    }
                } catch (Exception ex) {
                    System.out.println("Failed to query peer " + url + ": " + ex.getMessage());
                }
            }

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Patient not found in any instance (Mongo/SQL/Peers)"));
        }
    }


    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<PatientDetailsDTO>> listAllPatients() {
        return ResponseEntity.ok(patientService.listAllPatients());
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> searchPatients(@RequestParam String name) {
        try {
            return ResponseEntity.ok(patientService.searchPatientsByName(name));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/profile")
    @PreAuthorize("hasAuthority('PHYSICIAN')")
    public ResponseEntity<?> getPatientProfile(
            @PathVariable String id,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
        try {
            return ResponseEntity.ok(patientService.getPatientProfile(id, authorizationHeader));
        } catch (NotFoundException | EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/me")
    @PreAuthorize("hasAuthority('PATIENT')")
    public ResponseEntity<?> updateContactDetails(@Valid @RequestBody ContactDetailsUpdateDTO dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        String message = patientService.updateContactDetails(email, dto);
        return ResponseEntity.ok(new UpdateResponse(message));
    }

    @ExceptionHandler(UnrecognizedPropertyException.class)
    public ResponseEntity<ErrorResponse> handleUnrecognizedPropertyException(UnrecognizedPropertyException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse("Invalid request", "Unknown field: " + e.getPropertyName(), "BAD_REQUEST"));
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse("Invalid request", e.getMessage(), "BAD_REQUEST"));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        return ResponseEntity.badRequest().body(new ErrorResponse("Invalid request", e.getMessage(), "BAD_REQUEST"));
    }

    private record UpdateResponse(String message) {}
    private record ErrorResponse(String error, String message, String status) {}
}