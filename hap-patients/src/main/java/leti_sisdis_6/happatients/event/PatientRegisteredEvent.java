package leti_sisdis_6.happatients.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDate;
import leti_sisdis_6.happatients.dto.PatientRegistrationDTOV2;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PatientRegisteredEvent implements Serializable {
    private String patientId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private LocalDate birthDate;
    private Boolean dataConsentGiven;
    private LocalDate dataConsentDate;
    private AddressEventData address;
    private InsuranceEventData insuranceInfo;
    @Data @AllArgsConstructor @NoArgsConstructor
    public static class AddressEventData implements Serializable {
        private String street;
        private String city;
        private String postalCode;
        private String country;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class InsuranceEventData implements Serializable {
        private String policyNumber;
        private String provider;
        private String coverageType;
    }
}

