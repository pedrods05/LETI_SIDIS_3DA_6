package leti_sisdis_6.happhysicians.api;

import leti_sisdis_6.happhysicians.model.Physician;
import leti_sisdis_6.happhysicians.repository.PhysicianRepository;
import leti_sisdis_6.happhysicians.dto.request.RegisterPhysicianRequest;
import leti_sisdis_6.happhysicians.dto.request.UpdatePhysicianRequest;
import leti_sisdis_6.happhysicians.dto.response.AppointmentSlotDto;
import leti_sisdis_6.happhysicians.dto.response.PhysicianFullDTO;
import leti_sisdis_6.happhysicians.dto.response.PhysicianIdResponse;
import leti_sisdis_6.happhysicians.services.PhysicianService;
import leti_sisdis_6.happhysicians.services.ExternalServiceClient;
import leti_sisdis_6.happhysicians.command.PhysicianCommandService;
import leti_sisdis_6.happhysicians.query.PhysicianQueryService;
import leti_sisdis_6.happhysicians.util.SlotCalculator;
import leti_sisdis_6.happhysicians.repository.AppointmentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/physicians")
@Tag(name = "Physician", description = "Physician management endpoints")
public class PhysicianController {

    @Autowired
    private PhysicianRepository physicianRepository;

    @Autowired
    private PhysicianService physicianService;

    @Autowired
    private ExternalServiceClient externalServiceClient;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private PhysicianCommandService physicianCommandService;

    @Autowired
    private PhysicianQueryService physicianQueryService;

    @Autowired
    private SlotCalculator slotCalculator;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @GetMapping("/{physicianId}")
    @Operation(summary = "Get physician by ID")
    public ResponseEntity<Physician> getPhysician(@PathVariable String physicianId) {
        try {
            // Use Query Service (reads from MongoDB read model)
            Physician physician = physicianQueryService.getPhysicianById(physicianId);
            return ResponseEntity.ok(physician);
        } catch (Exception e) {
            // Fallback to peer forwarding if not found in read model
            List<String> peers = externalServiceClient.getPeerUrls();
            for (String peer : peers) {
                try {
                    Physician remotePhysician = restTemplate.getForObject(
                        peer + "/internal/physicians/" + physicianId, Physician.class);
                    if (remotePhysician != null) {
                        return ResponseEntity.ok(remotePhysician);
                    }
                } catch (Exception ex) {
                    System.out.println("Failed to query peer " + peer + ": " + ex.getMessage());
                }
            }
            return ResponseEntity.notFound().build();
        }
    }


    @PostMapping("/register")
    @Operation(summary = "Register a new physician")
    public ResponseEntity<?> registerPhysician(@RequestBody RegisterPhysicianRequest request) {
        try {
            // Use Command Service (writes to write model and publishes event)
            PhysicianIdResponse response = physicianCommandService.registerPhysician(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    @Operation(summary = "Get all physicians")
    public List<Physician> getAllPhysicians() {
        return physicianRepository.findAll();
    }

    @PutMapping("/{physicianId}")
    @Operation(summary = "Update physician by ID")
    public ResponseEntity<?> updatePhysician(@PathVariable String physicianId, @RequestBody UpdatePhysicianRequest request) {
        try {
            // Use Command Service (writes to write model and publishes event)
            PhysicianFullDTO updatedPhysician = physicianCommandService.updatePhysician(physicianId, request);
            return ResponseEntity.ok(updatedPhysician);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{physicianId}/slots")
    @Operation(summary = "Get available appointment slots for a physician")
    public ResponseEntity<List<AppointmentSlotDto>> getAvailableSlots(
            @PathVariable String physicianId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            // Get physician
            Physician physician = physicianQueryService.getPhysicianById(physicianId);
            if (physician == null) {
                return ResponseEntity.notFound().build();
            }

            // Parse dates or use defaults
            LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now();
            LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now().plusMonths(1);

            // Get existing appointments for the physician in the date range
            List<leti_sisdis_6.happhysicians.model.Appointment> appointments = appointmentRepository
                    .findByPhysicianPhysicianIdAndDateTimeBetween(
                            physicianId,
                            start.atStartOfDay(),
                            end.plusDays(1).atStartOfDay()
                    );

            // Generate available slots
            List<AppointmentSlotDto> slots = slotCalculator.generateAvailableSlots(physician, appointments, start, end);

            return ResponseEntity.ok(slots);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{physicianId}")
    @Operation(summary = "Delete physician by ID")
    public ResponseEntity<Void> deletePhysician(@PathVariable String physicianId) {
        if (physicianRepository.existsById(physicianId)) {
            physicianRepository.deleteById(physicianId);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/specialty/{specialtyId}")
    @Operation(summary = "Get physicians by specialty")
    public List<Physician> getPhysiciansBySpecialty(@PathVariable String specialtyId) {
        return physicianRepository.findBySpecialtySpecialtyId(specialtyId);
    }

    @GetMapping("/department/{departmentId}")
    @Operation(summary = "Get physicians by department")
    public List<Physician> getPhysiciansByDepartment(@PathVariable String departmentId) {
        return physicianRepository.findByDepartmentDepartmentId(departmentId);
    }
}