package leti_sisdis_6.happhysicians.query;

import leti_sisdis_6.happhysicians.exceptions.NotFoundException;
import leti_sisdis_6.happhysicians.model.Department;
import leti_sisdis_6.happhysicians.model.Physician;
import leti_sisdis_6.happhysicians.model.Specialty;
import leti_sisdis_6.happhysicians.repository.DepartmentRepository;
import leti_sisdis_6.happhysicians.repository.PhysicianRepository;
import leti_sisdis_6.happhysicians.repository.SpecialtyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PhysicianQueryService {

    private final PhysicianQueryRepository physicianQueryRepository;
    private final PhysicianRepository physicianRepository;
    private final SpecialtyRepository specialtyRepository;
    private final DepartmentRepository departmentRepository;

    public Physician getPhysicianById(String physicianId) {
        // Try to get from read model first (MongoDB - optimized for queries)
        Optional<PhysicianSummary> summaryOpt = physicianQueryRepository.findById(physicianId);
        
        if (summaryOpt.isPresent()) {
            PhysicianSummary summary = summaryOpt.get();
            // Convert read model to domain model, enriching with write model data if needed
            Physician physician = toPhysician(summary);
            
            // Try to enrich with additional data from write model (emails, phones, working hours, etc.)
            Optional<Physician> writeModelPhysician = physicianRepository.findById(physicianId);
            if (writeModelPhysician.isPresent()) {
                Physician fullPhysician = writeModelPhysician.get();
                // Preserve specialty and department from read model (they're already set)
                // But enrich with other fields from write model
                physician.setEmails(fullPhysician.getEmails());
                physician.setPhoneNumbers(fullPhysician.getPhoneNumbers());
                physician.setWorkingHourStart(fullPhysician.getWorkingHourStart());
                physician.setWorkingHourEnd(fullPhysician.getWorkingHourEnd());
                physician.setPhoto(fullPhysician.getPhoto());
                // Don't overwrite password for security
            }
            
            return physician;
        }
        
        // If not in read model, fallback to write model
        return physicianRepository.findById(physicianId)
                .orElseThrow(() -> new NotFoundException("Physician not found with ID: " + physicianId));
    }

    private Physician toPhysician(PhysicianSummary summary) {
        // Convert read model to domain model, including specialty and department
        Physician physician = new Physician();
        physician.setPhysicianId(summary.getId());
        physician.setFullName(summary.getFullName());
        physician.setLicenseNumber(summary.getLicenseNumber());
        physician.setUsername(summary.getUsername());
        
        // Set specialty from read model
        if (summary.getSpecialtyId() != null) {
            Specialty specialty = specialtyRepository.findById(summary.getSpecialtyId())
                    .orElseGet(() -> {
                        // If specialty not found in write model, create a minimal one from read model
                        Specialty s = new Specialty();
                        s.setSpecialtyId(summary.getSpecialtyId());
                        s.setName(summary.getSpecialtyName() != null ? summary.getSpecialtyName() : "Unknown");
                        return s;
                    });
            physician.setSpecialty(specialty);
        }
        
        // Set department from read model
        if (summary.getDepartmentId() != null) {
            Department department = departmentRepository.findById(summary.getDepartmentId())
                    .orElseGet(() -> {
                        // If department not found in write model, create a minimal one from read model
                        Department d = new Department();
                        d.setDepartmentId(summary.getDepartmentId());
                        d.setName(summary.getDepartmentName() != null ? summary.getDepartmentName() : "Unknown");
                        d.setCode("N/A");
                        return d;
                    });
            physician.setDepartment(department);
        }
        
        return physician;
    }
}

