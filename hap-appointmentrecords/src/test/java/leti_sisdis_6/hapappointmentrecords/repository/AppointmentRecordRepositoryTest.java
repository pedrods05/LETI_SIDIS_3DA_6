package leti_sisdis_6.hapappointmentrecords.repository;

import leti_sisdis_6.hapappointmentrecords.model.AppointmentRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class AppointmentRecordRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AppointmentRecordRepository repository;

    private AppointmentRecord record1;
    private AppointmentRecord record2;

    @BeforeEach
    void setUp() {
        // Note: Appointments now live in physicians service
        // We only store appointmentId references

        record1 = AppointmentRecord.builder()
                .recordId("REC001")
                .appointmentId("APT001") // reference to appointment in physicians service
                .diagnosis("Gripe comum")
                .treatmentRecommendations("Repouso e hidratação")
                .prescriptions("Paracetamol 500mg")
                .duration(LocalTime.of(0, 30))
                .build();

        record2 = AppointmentRecord.builder()
                .recordId("REC002")
                .appointmentId("APT002")
                .diagnosis("Pressão alta")
                .treatmentRecommendations("Dieta com baixo teor de sódio")
                .prescriptions("Losartana 50mg")
                .duration(LocalTime.of(0, 45))
                .build();

        entityManager.persistAndFlush(record1);
        entityManager.persistAndFlush(record2);
        entityManager.clear();
    }

    @Test
    @DisplayName("Deve salvar e recuperar AppointmentRecord")
    void shouldSaveAndRetrieveAppointmentRecord() {
        // Given
        AppointmentRecord newRecord = AppointmentRecord.builder()
                .recordId("REC003")
                .appointmentId("APT003")
                .diagnosis("Diabetes tipo 2")
                .treatmentRecommendations("Controle glicêmico")
                .prescriptions("Metformina 850mg")
                .duration(LocalTime.of(1, 0))
                .build();

        // When
        AppointmentRecord saved = repository.save(newRecord);
        Optional<AppointmentRecord> retrieved = repository.findById(saved.getRecordId());

        // Then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getRecordId()).isEqualTo("REC003");
        assertThat(retrieved.get().getDiagnosis()).isEqualTo("Diabetes tipo 2");
        assertThat(retrieved.get().getAppointmentId()).isEqualTo("APT003");
    }

    @Test
    @DisplayName("Deve encontrar record por appointment ID")
    void shouldFindRecordByAppointmentId() {
        // When
        Optional<AppointmentRecord> record = repository.findByAppointmentId("APT001");

        // Then
        assertThat(record).isPresent();
        assertThat(record.get().getRecordId()).isEqualTo("REC001");
        assertThat(record.get().getDiagnosis()).isEqualTo("Gripe comum");
    }

    @Test
    @DisplayName("Deve retornar empty para appointment ID inexistente")
    void shouldReturnEmptyForNonExistentAppointmentId() {
        // When
        Optional<AppointmentRecord> record = repository.findByAppointmentId("APT999");

        // Then
        assertThat(record).isEmpty();
    }

    @Test
    @DisplayName("Deve buscar records por múltiplos appointment IDs")
    void shouldFindRecordsByMultipleAppointmentIds() {
        // When
        Optional<AppointmentRecord> record1 = repository.findByAppointmentId("APT001");
        Optional<AppointmentRecord> record2 = repository.findByAppointmentId("APT002");

        // Then
        assertThat(record1).isPresent();
        assertThat(record1.get().getRecordId()).isEqualTo("REC001");

        assertThat(record2).isPresent();
        assertThat(record2.get().getRecordId()).isEqualTo("REC002");
    }

    @Test
    @DisplayName("Deve contar total de records")
    void shouldCountTotalRecords() {
        // When
        long count = repository.count();

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Deve deletar record por ID")
    void shouldDeleteRecordById() {
        // Given
        assertThat(repository.existsById("REC001")).isTrue();

        // When
        repository.deleteById("REC001");
        entityManager.flush();

        // Then
        assertThat(repository.existsById("REC001")).isFalse();
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve atualizar record existente")
    void shouldUpdateExistingRecord() {
        // Given
        Optional<AppointmentRecord> found = repository.findById("REC001");
        assertThat(found).isPresent();

        AppointmentRecord record = found.get();
        record.setDiagnosis("Gripe H1N1");
        record.setPrescriptions("Tamiflu 75mg");

        // When
        AppointmentRecord updated = repository.save(record);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<AppointmentRecord> retrieved = repository.findById("REC001");
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getDiagnosis()).isEqualTo("Gripe H1N1");
        assertThat(retrieved.get().getPrescriptions()).isEqualTo("Tamiflu 75mg");
    }

    @Test
    @DisplayName("Deve validar constraints de campos obrigatórios")
    void shouldValidateMandatoryFieldConstraints() {
        // Given
        AppointmentRecord record = AppointmentRecord.builder()
                .recordId("REC-CONSTRAINTS")
                .appointmentId("APT-CONSTRAINTS")
                .diagnosis("Teste de constraints")
                .treatmentRecommendations("Teste")
                .prescriptions("Teste")
                .duration(LocalTime.of(0, 30))
                .build();

        // When
        AppointmentRecord saved = repository.save(record);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getRecordId()).isEqualTo("REC-CONSTRAINTS");
        assertThat(saved.getAppointmentId()).isEqualTo("APT-CONSTRAINTS");
        assertThat(saved.getDiagnosis()).isNotNull();
    }
}
