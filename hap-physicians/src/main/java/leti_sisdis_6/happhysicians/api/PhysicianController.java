package leti_sisdis_6.happhysicians.api;

import leti_sisdis_6.happhysicians.model.Physician;
import leti_sisdis_6.happhysicians.repository.PhysicianRepository;
import leti_sisdis_6.happhysicians.dto.request.RegisterPhysicianRequest;
import leti_sisdis_6.happhysicians.dto.response.PhysicianIdResponse;
import leti_sisdis_6.happhysicians.services.PhysicianService;
import leti_sisdis_6.happhysicians.services.ExternalServiceClient;
import leti_sisdis_6.happhysicians.command.PhysicianCommandService;
import leti_sisdis_6.happhysicians.query.PhysicianQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<Physician> updatePhysician(@PathVariable String physicianId, @RequestBody Physician physicianDetails) {
        Optional<Physician> optionalPhysician = physicianRepository.findById(physicianId);
        if (optionalPhysician.isPresent()) {
            Physician physician = optionalPhysician.get();
            physician.setFullName(physicianDetails.getFullName());
            physician.setLicenseNumber(physicianDetails.getLicenseNumber());
            physician.setUsername(physicianDetails.getUsername());
            physician.setPassword(physicianDetails.getPassword());
            physician.setSpecialty(physicianDetails.getSpecialty());
            physician.setDepartment(physicianDetails.getDepartment());
            physician.setEmails(physicianDetails.getEmails());
            physician.setPhoneNumbers(physicianDetails.getPhoneNumbers());
            physician.setWorkingHourStart(physicianDetails.getWorkingHourStart());
            physician.setWorkingHourEnd(physicianDetails.getWorkingHourEnd());
            physician.setPhoto(physicianDetails.getPhoto());
            
            Physician updatedPhysician = physicianRepository.save(physician);
            return ResponseEntity.ok(updatedPhysician);
        } else {
            return ResponseEntity.notFound().build();
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