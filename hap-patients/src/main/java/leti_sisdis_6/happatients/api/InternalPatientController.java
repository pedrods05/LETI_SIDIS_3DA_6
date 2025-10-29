package leti_sisdis_6.happatients.api;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import leti_sisdis_6.happatients.dto.PatientDetailsDTO;
import leti_sisdis_6.happatients.repository.PatientLocalRepository;
import leti_sisdis_6.happatients.service.PatientService;

@RestController
@RequestMapping("/internal/patients")
@RequiredArgsConstructor
@Hidden
public class InternalPatientController {

    private final PatientService patientService;
    private final PatientLocalRepository localRepo;

    @GetMapping("/{id}")
    public ResponseEntity<?> getPatientInternal(@PathVariable String id) {
        PatientDetailsDTO cached = localRepo.findById(id).orElse(null);
        if (cached != null) {
            return ResponseEntity.ok(cached);
        }
        try {
            PatientDetailsDTO patient = patientService.getPatientDetails(id);
            localRepo.save(patient);
            return ResponseEntity.ok(patient);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Patient not found"));
        }
    }
}

