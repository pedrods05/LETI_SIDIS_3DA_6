package leti_sisdis_6.happhysicians.api;

import leti_sisdis_6.happhysicians.model.Physician;
import leti_sisdis_6.happhysicians.repository.PhysicianRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/physicians")
public class PhysicianController {

    @Autowired
    private PhysicianRepository physicianRepository;

    @GetMapping("/{physicianId}")
    public ResponseEntity<Physician> getPhysician(@PathVariable String physicianId) {
        return physicianRepository.findById(physicianId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Physician createPhysician(@RequestBody Physician physician) {
        return physicianRepository.save(physician);
    }

    @GetMapping
    public List<Physician> getAllPhysicians() {
        return physicianRepository.findAll();
    }

    @PutMapping("/{physicianId}")
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
    public ResponseEntity<Void> deletePhysician(@PathVariable String physicianId) {
        if (physicianRepository.existsById(physicianId)) {
            physicianRepository.deleteById(physicianId);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/specialty/{specialtyId}")
    public List<Physician> getPhysiciansBySpecialty(@PathVariable String specialtyId) {
        return physicianRepository.findBySpecialtySpecialtyId(specialtyId);
    }

    @GetMapping("/department/{departmentId}")
    public List<Physician> getPhysiciansByDepartment(@PathVariable String departmentId) {
        return physicianRepository.findByDepartmentDepartmentId(departmentId);
    }
}