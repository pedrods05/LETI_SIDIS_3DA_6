package leti_sisdis_6.happatients.query;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PatientSummaryTest {

    @Test
    void createPatientSummary_shouldSetAllFields() {
        // Arrange
        PatientSummary.AddressSummary address = new PatientSummary.AddressSummary(
                "Rua das Flores 123",
                "Porto",
                "4000-123",
                "Portugal"
        );

        PatientSummary.InsuranceSummary insurance = new PatientSummary.InsuranceSummary(
                "POL123456",
                "Seguradora XYZ",
                "Premium"
        );

        List<String> healthConcerns = Arrays.asList("Hypertension", "Diabetes");
        LocalDate birthDate = LocalDate.of(1990, 5, 15);
        LocalDate consentDate = LocalDate.now();

        // Act
        PatientSummary summary = new PatientSummary(
                "PAT01",
                "Joao Silva",
                "joao.silva@example.com",
                "912345678",
                birthDate,
                true,
                consentDate,
                address,
                insurance,
                healthConcerns
        );

        // Assert
        assertThat(summary.getPatientId()).isEqualTo("PAT01");
        assertThat(summary.getFullName()).isEqualTo("Joao Silva");
        assertThat(summary.getEmail()).isEqualTo("joao.silva@example.com");
        assertThat(summary.getPhoneNumber()).isEqualTo("912345678");
        assertThat(summary.getBirthDate()).isEqualTo(birthDate);
        assertThat(summary.getDataConsentGiven()).isTrue();
        assertThat(summary.getDataConsentDate()).isEqualTo(consentDate);
        assertThat(summary.getAddress()).isEqualTo(address);
        assertThat(summary.getInsuranceInfo()).isEqualTo(insurance);
        assertThat(summary.getHealthConcerns()).containsExactly("Hypertension", "Diabetes");
    }

    @Test
    void addressSummary_shouldStoreAddressDetails() {
        // Act
        PatientSummary.AddressSummary address = new PatientSummary.AddressSummary(
                "Avenida Central 456",
                "Lisboa",
                "1000-001",
                "Portugal"
        );

        // Assert
        assertThat(address.getStreet()).isEqualTo("Avenida Central 456");
        assertThat(address.getCity()).isEqualTo("Lisboa");
        assertThat(address.getPostalCode()).isEqualTo("1000-001");
        assertThat(address.getCountry()).isEqualTo("Portugal");
    }

    @Test
    void insuranceSummary_shouldStoreInsuranceDetails() {
        // Act
        PatientSummary.InsuranceSummary insurance = new PatientSummary.InsuranceSummary(
                "POL999888",
                "HealthCare Pro",
                "Basic"
        );

        // Assert
        assertThat(insurance.getPolicyNumber()).isEqualTo("POL999888");
        assertThat(insurance.getProvider()).isEqualTo("HealthCare Pro");
        assertThat(insurance.getCoverageType()).isEqualTo("Basic");
    }

    @Test
    void noArgsConstructor_shouldCreateEmptySummary() {
        // Act
        PatientSummary summary = new PatientSummary();

        // Assert
        assertThat(summary.getPatientId()).isNull();
        assertThat(summary.getFullName()).isNull();
        assertThat(summary.getEmail()).isNull();
        assertThat(summary.getHealthConcerns()).isNull();
    }

    @Test
    void settersAndGetters_shouldWorkCorrectly() {
        // Arrange
        PatientSummary summary = new PatientSummary();

        // Act
        summary.setPatientId("PAT02");
        summary.setFullName("Maria Santos");
        summary.setEmail("maria@example.com");
        summary.setPhoneNumber("923456789");
        summary.setBirthDate(LocalDate.of(1985, 3, 20));
        summary.setDataConsentGiven(false);

        // Assert
        assertThat(summary.getPatientId()).isEqualTo("PAT02");
        assertThat(summary.getFullName()).isEqualTo("Maria Santos");
        assertThat(summary.getEmail()).isEqualTo("maria@example.com");
        assertThat(summary.getPhoneNumber()).isEqualTo("923456789");
        assertThat(summary.getBirthDate()).isEqualTo(LocalDate.of(1985, 3, 20));
        assertThat(summary.getDataConsentGiven()).isFalse();
    }

    @Test
    void healthConcerns_shouldBeModifiable() {
        // Arrange
        PatientSummary summary = new PatientSummary();
        List<String> concerns = Arrays.asList("Asthma", "Allergies");

        // Act
        summary.setHealthConcerns(concerns);

        // Assert
        assertThat(summary.getHealthConcerns()).hasSize(2);
        assertThat(summary.getHealthConcerns()).contains("Asthma", "Allergies");
    }
}

