package leti_sisdis_6.happatients.api;

import leti_sisdis_6.happatients.dto.PatientDetailsDTO;
import leti_sisdis_6.happatients.service.PatientService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/internal/patients")
@RequiredArgsConstructor
public class InternalPatientController {

    private final PatientService patientService;

    @GetMapping(value = "/{id}", produces = "application/json")
    public ResponseEntity<?> getPatientDetailsInternal(@PathVariable String id) {
        try {
            PatientDetailsDTO patient = patientService.getPatientDetails(id);
            if (patient != null) {
                return ResponseEntity.ok(patient);
            }
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Patient not found", "patientId", id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage(), "patientId", id));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Patient not found", "patientId", id));
    }
}

