package leti_sisdis_6.happatients.query;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "patient_summaries")
public class PatientSummary {
    @Id
    private String patientId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private LocalDate birthDate;
    private Boolean dataConsentGiven;
    private LocalDate dataConsentDate;
    private AddressSummary address;
    private InsuranceSummary insuranceInfo;
    private List<String> healthConcerns;
    @Data @AllArgsConstructor @NoArgsConstructor
    public static class AddressSummary {
        private String street;
        private String city;
        private String postalCode;
        private String country;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class InsuranceSummary {
        private String policyNumber;
        private String provider;
        private String coverageType;
    }
}
