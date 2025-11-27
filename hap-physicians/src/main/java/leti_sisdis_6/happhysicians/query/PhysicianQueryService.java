package leti_sisdis_6.happhysicians.query;

import leti_sisdis_6.happhysicians.exceptions.NotFoundException;
import leti_sisdis_6.happhysicians.model.Physician;
import leti_sisdis_6.happhysicians.repository.PhysicianRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PhysicianQueryService {

    private final PhysicianQueryRepository physicianQueryRepository;
    private final PhysicianRepository physicianRepository;

    public Physician getPhysicianById(String physicianId) {
        // Try to get from read model first (fast)
        PhysicianSummary summary = physicianQueryRepository.findById(physicianId).orElse(null);
        
        if (summary != null) {
            // If found in read model, try to enrich with write model data
            // Fallback to write model if read model doesn't have all data
            return physicianRepository.findById(physicianId)
                    .orElseGet(() -> {
                        // If not in write model, return basic data from read model
                        return toPhysician(summary);
                    });
        }
        
        // If not in read model, fallback to write model
        return physicianRepository.findById(physicianId)
                .orElseThrow(() -> new NotFoundException("Physician not found with ID: " + physicianId));
    }

    private Physician toPhysician(PhysicianSummary summary) {
        // Convert read model to domain model (minimal conversion)
        Physician physician = new Physician();
        physician.setPhysicianId(summary.getId());
        physician.setFullName(summary.getFullName());
        physician.setLicenseNumber(summary.getLicenseNumber());
        physician.setUsername(summary.getUsername());
        return physician;
    }
}

