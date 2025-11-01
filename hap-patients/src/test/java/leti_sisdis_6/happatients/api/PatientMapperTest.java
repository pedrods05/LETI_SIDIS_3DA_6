package leti_sisdis_6.happatients.api;

import leti_sisdis_6.happatients.dto.PatientDetailsDTO;
import leti_sisdis_6.happatients.model.Address;
import leti_sisdis_6.happatients.model.InsuranceInfo;
import leti_sisdis_6.happatients.model.Patient;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PatientMapperTest {

    private final PatientMapper mapper = new PatientMapper();

    @Test
    void toDetailsDTO_mapsAll() {
        Patient p = Patient.builder()
                .patientId("PAT01")
                .fullName("Alice")
                .email("a@a")
                .birthDate(LocalDate.of(2000,1,1))
                .phoneNumber("9")
                .dataConsentGiven(true)
                .dataConsentDate(LocalDate.now())
                .address(Address.builder().street("S").city("C").postalCode("P").country("PT").build())
                .insuranceInfo(InsuranceInfo.builder().policyNumber("X").provider("Prov").coverageType("Basic").build())
                .build();

        PatientDetailsDTO dto = mapper.toDetailsDTO(p);
        assertThat(dto.getPatientId()).isEqualTo("PAT01");
        assertThat(dto.getAddress().getCity()).isEqualTo("C");
        assertThat(dto.getInsuranceInfo().getProvider()).isEqualTo("Prov");
    }

    @Test
    void toDetailsDTO_null_safe() {
        assertThat(mapper.toDetailsDTO(null)).isNull();
    }
}

