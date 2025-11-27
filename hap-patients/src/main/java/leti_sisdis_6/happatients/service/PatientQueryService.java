package leti_sisdis_6.happatients.service;

import leti_sisdis_6.happatients.dto.PatientProfileDTO;
import leti_sisdis_6.happatients.exceptions.NotFoundException;
import leti_sisdis_6.happatients.query.PatientSummary;
import leti_sisdis_6.happatients.query.PatientQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PatientQueryService {

    private final PatientQueryRepository patientQueryRepository;

    public PatientProfileDTO getPatientProfile(String id) {
        return patientQueryRepository.findById(id)
                .map(this::toProfileDTO)
                .orElseThrow(() -> new NotFoundException("Patient not found with ID: " + id));
    }

    private PatientProfileDTO toProfileDTO(PatientSummary summary) {
        return PatientProfileDTO.builder()
                .patientId(summary.getId())
                .fullName(summary.getName())
                .email(summary.getEmail())
                .build();
    }
}
