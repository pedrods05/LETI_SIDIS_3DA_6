package leti_sisdis_6.happhysicians.dto.external;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientDTO {
    private String patientId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private AddressDTO address;
    private List<String> healthConcerns;
    private Boolean dataConsentGiven;
    private InsuranceInfoDTO insuranceInfo;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressDTO {
        private String id;
        private String street;
        private String city;
        private String postalCode;
        private String country;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InsuranceInfoDTO {
        private String id;
        private String policyNumber;
        private String provider;
        private String coverageType;
    }
}
