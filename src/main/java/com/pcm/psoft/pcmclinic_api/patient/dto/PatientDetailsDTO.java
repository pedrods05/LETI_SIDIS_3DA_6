package com.pcm.psoft.pcmclinic_api.patient.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientDetailsDTO {
    private String patientId;
    private String fullName;
    private LocalDate birthDate;
    private String phoneNumber;
    private String email;
    private boolean dataConsentGiven;
    private LocalDate dataConsentDate;
    private List<String> healthConcerns;
    private AddressDTO address;
    private InsuranceInfoDTO insuranceInfo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressDTO {
        private String street;
        private String city;
        private String postalCode;
        private String country;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InsuranceInfoDTO {
        private String policyNumber;
        private String provider;
        private String coverageType;
    }
} 