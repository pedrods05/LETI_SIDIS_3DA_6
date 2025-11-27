package leti_sisdis_6.happatients.repository;

import leti_sisdis_6.happatients.dto.PatientDetailsDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PatientLocalRepositoryTest {

    private PatientLocalRepository repo;

    @BeforeEach
    void setup() { repo = new PatientLocalRepository(); }

    @Test
    void save_and_find_work() {
        PatientDetailsDTO dto = PatientDetailsDTO.builder().patientId("PAT01").fullName("Alice").email("a@a").build();
        repo.save(dto);
        assertThat(repo.findById("PAT01")).isPresent();
        assertThat(repo.findAll()).hasSize(1);
    }

    @Test
    void clear_removes_all() {
        repo.save(PatientDetailsDTO.builder().patientId("PAT01").build());
        repo.save(PatientDetailsDTO.builder().patientId("PAT02").build());
        assertThat(repo.findAll()).hasSize(2);
        repo.clear();
        assertThat(repo.findAll()).isEmpty();
    }

    @Test
    void save_null_or_missing_id_throws() {
        assertThrows(IllegalArgumentException.class, () -> repo.save(null));
        assertThrows(IllegalArgumentException.class, () -> repo.save(PatientDetailsDTO.builder().build()));
    }
}

