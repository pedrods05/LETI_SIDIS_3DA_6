package leti_sisdis_6.happatients.api;

import leti_sisdis_6.happatients.model.Patient;
import leti_sisdis_6.happatients.model.HealthConcern;
import leti_sisdis_6.happatients.dto.PatientDetailsDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PatientMapper {

    public PatientDetailsDTO toDetailsDTO(Patient patient) {
        if (patient == null) {
            return null;
        }

        PatientDetailsDTO dto = new PatientDetailsDTO();
        dto.setPatientId(patient.getPatientId());
        dto.setFullName(patient.getFullName());
        dto.setEmail(patient.getEmail());
        dto.setPhoneNumber(patient.getPhoneNumber());
        dto.setBirthDate(patient.getBirthDate());
        dto.setDataConsentGiven(patient.isDataConsentGiven());
        dto.setDataConsentDate(patient.getDataConsentDate());
        
        if (patient.getAddress() != null) {
            dto.setAddress(toAddressDTO(patient.getAddress()));
        }
        
        if (patient.getInsuranceInfo() != null) {
            dto.setInsuranceInfo(toInsuranceInfoDTO(patient.getInsuranceInfo()));
        }
        
        if (patient.getHealthConcerns() != null) {
            dto.setHealthConcerns(healthConcernsToDescriptions(patient.getHealthConcerns()));
        }

        return dto;
    }

    protected List<String> healthConcernsToDescriptions(List<HealthConcern> healthConcerns) {
        if (healthConcerns == null) {
            return null;
        }
        return healthConcerns.stream()
                .map(HealthConcern::getDescription)
                .collect(Collectors.toList());
    }

    public PatientDetailsDTO.AddressDTO toAddressDTO(com.pcm.psoft.pcmclinic_api.patient.model.Address address) {
        if (address == null) {
            return null;
        }

        PatientDetailsDTO.AddressDTO dto = new PatientDetailsDTO.AddressDTO();
        dto.setStreet(address.getStreet());
        dto.setCity(address.getCity());
        dto.setPostalCode(address.getPostalCode());
        dto.setCountry(address.getCountry());
        return dto;
    }

    public PatientDetailsDTO.InsuranceInfoDTO toInsuranceInfoDTO(com.pcm.psoft.pcmclinic_api.patient.model.InsuranceInfo insuranceInfo) {
        if (insuranceInfo == null) {
            return null;
        }

        PatientDetailsDTO.InsuranceInfoDTO dto = new PatientDetailsDTO.InsuranceInfoDTO();
        dto.setPolicyNumber(insuranceInfo.getPolicyNumber());
        dto.setProvider(insuranceInfo.getProvider());
        dto.setCoverageType(insuranceInfo.getCoverageType());
        return dto;
    }
} 