package leti_sisdis_6.happhysicians.api;

import leti_sisdis_6.happhysicians.dto.input.RegisterPhysicianRequest;
import leti_sisdis_6.happhysicians.dto.input.UpdatePhysicianRequest;
import leti_sisdis_6.happhysicians.dto.output.PhysicianIdResponse;
import leti_sisdis_6.happhysicians.dto.output.PhysicianLimitedDTO;
import leti_sisdis_6.happhysicians.dto.output.PhysicianFullDTO;
import leti_sisdis_6.happhysicians.services.PhysicianService;
import leti_sisdis_6.happhysicians.dto.output.TopPhysicianDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/physicians")
@RequiredArgsConstructor
@Tag(name = "Physician", description = "Physician management endpoints")
public class PhysicianController {

    private final PhysicianService physicianService;

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
            summary = "Register new physician",
            description = "Creates a new physician account with their specialty and department information",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Physician registered successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request data"),
                    @ApiResponse(responseCode = "409", description = "Username or license number already exists"),
                    @ApiResponse(responseCode = "403", description = "Access forbidden")
            }
    )
    public ResponseEntity<PhysicianIdResponse> registerPhysician(@RequestBody @Valid RegisterPhysicianRequest request) {
        PhysicianIdResponse response = physicianService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
            summary = "Register new physician with optional photo",
            description = "Creates a new physician with optional profile photo (multipart)",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Physician registered successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request data"),
                    @ApiResponse(responseCode = "409", description = "Username or license number already exists"),
                    @ApiResponse(responseCode = "403", description = "Access forbidden")
            }
    )
    public ResponseEntity<PhysicianIdResponse> registerPhysicianWithPhoto(
            @RequestPart("data") @Valid RegisterPhysicianRequest request,
            @RequestPart(value = "photo", required = false) MultipartFile photo) {
        PhysicianIdResponse response = physicianService.registerWithPhoto(request, photo);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get physician details",
            description = "Retrieves physician details based on the user role. Administrators get full details while patients get limited information.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved physician details"),
                    @ApiResponse(responseCode = "404", description = "Physician not found"),
                    @ApiResponse(responseCode = "403", description = "Access forbidden")
            }
    )
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('PATIENT')")
    public ResponseEntity<?> getPhysicianDetails(@PathVariable String id) {
        try {
            return ResponseEntity.ok(physicianService.getPhysicianDetails(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/searchByNameOrSpecialty")
    @Operation(
            summary = "Search physicians by name or specialty",
            description = "Search for physicians by name or specialty. Name search requires at least 3 characters. Returns a list of physicians matching the criteria.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved physicians"),
                    @ApiResponse(responseCode = "400", description = "Name search requires at least 3 characters"),
                    @ApiResponse(responseCode = "404", description = "No physicians found"),
                    @ApiResponse(responseCode = "403", description = "Access forbidden")
            }
    )
    @PreAuthorize("hasAuthority('PATIENT')")
    public ResponseEntity<List<PhysicianLimitedDTO>> searchPhysicians(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String specialty) {

        List<PhysicianLimitedDTO> result = physicianService.searchByNameOrSpecialty(name, specialty);

        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
            summary = "Update physician data",
            description = "Partially updates a physician's data. Only fields present in the request will be updated.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Physician updated successfully"),
                    @ApiResponse(responseCode = "404", description = "Physician, department, or specialty not found"),
                    @ApiResponse(responseCode = "400", description = "Invalid request data"),
                    @ApiResponse(responseCode = "409", description = "License number conflict"),
                    @ApiResponse(responseCode = "403", description = "Access forbidden")
            }
    )
    public ResponseEntity<PhysicianFullDTO> updatePhysician(
            @PathVariable String id,
            @RequestBody UpdatePhysicianRequest request
    ) {
        try {
            PhysicianFullDTO updated = physicianService.partialUpdate(id, request);
            return ResponseEntity.ok(updated);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Physician, department, or specialty not found", e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request data", e);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "License number conflict", e);
        }
    }

    @GetMapping("/top5")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
            summary = "Get top 5 physicians by appointment count",
            description = "Returns the top 5 physicians with the most appointments in a given period.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved top 5 physicians"),
                    @ApiResponse(responseCode = "400", description = "Invalid date format"),
                    @ApiResponse(responseCode = "404", description = "No physicians found in the given period"),
                    @ApiResponse(responseCode = "403", description = "Access forbidden")
            }
    )
    public ResponseEntity<List<TopPhysicianDTO>> getTop5Physicians(@RequestParam("from") String from,
                                                                   @RequestParam("to") String to) {
        try {
            LocalDateTime fromDate = LocalDate.parse(from).atStartOfDay();
            LocalDateTime toDate = LocalDate.parse(to).atTime(23, 59, 59);
            List<TopPhysicianDTO> result = physicianService.getTop5Physicians(fromDate, toDate);
            if (result.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            return ResponseEntity.ok(result);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date format. Use yyyy-MM-dd.", e);
        }
    }
}
