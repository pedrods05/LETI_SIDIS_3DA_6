package leti_sisdis_6.happatients.service;

import leti_sisdis_6.happatients.dto.PatientProfileDTO;
import leti_sisdis_6.happatients.exceptions.NotFoundException;
import leti_sisdis_6.happatients.query.PatientSummary;
import leti_sisdis_6.happatients.query.PatientQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Collections;

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

        PatientProfileDTO.AddressDTO addressDTO = null;
        if (summary.getAddress() != null) {
            addressDTO = PatientProfileDTO.AddressDTO.builder()
                    .street(summary.getAddress().getStreet())
                    .city(summary.getAddress().getCity())
                    .postalCode(summary.getAddress().getPostalCode())
                    .country(summary.getAddress().getCountry())
                    .build();
        }

        PatientProfileDTO.InsuranceInfoDTO insuranceDTO = null;
        if (summary.getInsuranceInfo() != null) {
            insuranceDTO = PatientProfileDTO.InsuranceInfoDTO.builder()
                    .policyNumber(summary.getInsuranceInfo().getPolicyNumber())
                    .provider(summary.getInsuranceInfo().getProvider())
                    .coverageType(summary.getInsuranceInfo().getCoverageType())
                    .build();
        }

        return PatientProfileDTO.builder()
                .patientId(summary.getPatientId())
                .fullName(summary.getFullName())
                .email(summary.getEmail())
                .phoneNumber(summary.getPhoneNumber())
                .birthDate(summary.getBirthDate())
                .dataConsentGiven(summary.getDataConsentGiven())
                .dataConsentDate(summary.getDataConsentDate())
                .healthConcerns(summary.getHealthConcerns() != null ? summary.getHealthConcerns() : Collections.emptyList())
                .address(addressDTO)
                .insuranceInfo(insuranceDTO)
                .build();
    }
}
