package leti_sisdis_6.happatients.repository;

import leti_sisdis_6.happatients.model.Patient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class PatientRepositoryTest {

    @Autowired private PatientRepository repo;

    @Test
    void findByFullNameContainingIgnoreCase_works() {
        repo.save(Patient.builder()
                .patientId("PAT01")
                .fullName("Alice Smith")
                .email("a@a")
                .phoneNumber("+351900000000")
                .birthDate(LocalDate.now())
                .dataConsentGiven(true)
                .dataConsentDate(LocalDate.now())
                .build());

        List<Patient> res = repo.findByFullNameContainingIgnoreCase("ali");
        assertThat(res).hasSize(1);
    }
}

