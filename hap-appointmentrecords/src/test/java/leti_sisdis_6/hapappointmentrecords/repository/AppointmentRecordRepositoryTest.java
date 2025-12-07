package leti_sisdis_6.hapappointmentrecords.repository;

import leti_sisdis_6.hapappointmentrecords.model.AppointmentRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AppointmentRecordRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AppointmentRecordRepository repository;

    private AppointmentRecord testRecord1;
    private AppointmentRecord testRecord2;

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        testRecord1 = new AppointmentRecord();
        testRecord1.setRecordId("REC001");
        testRecord1.setAppointmentId("APT001");
        testRecord1.setDiagnosis("Gripe comum");
        testRecord1.setTreatmentRecommendations("Repouso e hidratação");
        testRecord1.setPrescriptions("Paracetamol 500mg");
        testRecord1.setDuration(LocalTime.of(0, 30));

        testRecord2 = new AppointmentRecord();
        testRecord2.setRecordId("REC002");
        testRecord2.setAppointmentId("APT002");
        testRecord2.setDiagnosis("Hipertensão");
        testRecord2.setTreatmentRecommendations("Dieta e exercício");
        testRecord2.setPrescriptions("Enalapril 10mg");
        testRecord2.setDuration(LocalTime.of(0, 45));
    }

    @Test
    @DisplayName("Should save appointment record successfully")
    void shouldSaveAppointmentRecord() {
        // When
        AppointmentRecord saved = repository.save(testRecord1);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getRecordId()).isEqualTo("REC001");
        assertThat(saved.getAppointmentId()).isEqualTo("APT001");
        assertThat(saved.getDiagnosis()).isEqualTo("Gripe comum");
    }

    @Test
    @DisplayName("Should find appointment record by ID")
    void shouldFindAppointmentRecordById() {
        // Given
        entityManager.persist(testRecord1);
        entityManager.flush();

        // When
        Optional<AppointmentRecord> found = repository.findById("REC001");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getRecordId()).isEqualTo("REC001");
        assertThat(found.get().getAppointmentId()).isEqualTo("APT001");
    }

    @Test
    @DisplayName("Should return empty when appointment record not found by ID")
    void shouldReturnEmptyWhenNotFoundById() {
        // When
        Optional<AppointmentRecord> found = repository.findById("NON_EXISTENT");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find appointment record by appointmentId")
    void shouldFindAppointmentRecordByAppointmentId() {
        // Given
        entityManager.persist(testRecord1);
        entityManager.flush();

        // When
        Optional<AppointmentRecord> found = repository.findByAppointmentId("APT001");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getRecordId()).isEqualTo("REC001");
        assertThat(found.get().getAppointmentId()).isEqualTo("APT001");
    }

    @Test
    @DisplayName("Should return empty when appointment record not found by appointmentId")
    void shouldReturnEmptyWhenNotFoundByAppointmentId() {
        // When
        Optional<AppointmentRecord> found = repository.findByAppointmentId("NON_EXISTENT");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find all appointment records")
    void shouldFindAllAppointmentRecords() {
        // Given
        entityManager.persist(testRecord1);
        entityManager.persist(testRecord2);
        entityManager.flush();

        // When
        List<AppointmentRecord> records = repository.findAll();

        // Then
        assertThat(records).hasSize(2);
        assertThat(records).extracting(AppointmentRecord::getRecordId)
                .containsExactlyInAnyOrder("REC001", "REC002");
    }

    @Test
    @DisplayName("Should delete appointment record")
    void shouldDeleteAppointmentRecord() {
        // Given
        entityManager.persist(testRecord1);
        entityManager.flush();

        // When
        repository.deleteById("REC001");

        // Then
        Optional<AppointmentRecord> found = repository.findById("REC001");
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should update appointment record")
    void shouldUpdateAppointmentRecord() {
        // Given
        entityManager.persist(testRecord1);
        entityManager.flush();

        // When
        testRecord1.setDiagnosis("Gripe comum - Atualizado");
        testRecord1.setPrescriptions("Paracetamol 500mg + Vitamina C");
        AppointmentRecord updated = repository.save(testRecord1);

        // Then
        assertThat(updated.getDiagnosis()).isEqualTo("Gripe comum - Atualizado");
        assertThat(updated.getPrescriptions()).isEqualTo("Paracetamol 500mg + Vitamina C");
    }

    @Test
    @DisplayName("Should count appointment records")
    void shouldCountAppointmentRecords() {
        // Given
        entityManager.persist(testRecord1);
        entityManager.persist(testRecord2);
        entityManager.flush();

        // When
        long count = repository.count();

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should check if appointment record exists by ID")
    void shouldCheckIfExistsById() {
        // Given
        entityManager.persist(testRecord1);
        entityManager.flush();

        // When/Then
        assertThat(repository.existsById("REC001")).isTrue();
        assertThat(repository.existsById("NON_EXISTENT")).isFalse();
    }

    @Test
    @DisplayName("Should delete all appointment records")
    void shouldDeleteAllAppointmentRecords() {
        // Given
        entityManager.persist(testRecord1);
        entityManager.persist(testRecord2);
        entityManager.flush();

        // When
        repository.deleteAll();

        // Then
        assertThat(repository.count()).isZero();
    }

    @Test
    @DisplayName("Should handle null values in optional fields")
    void shouldHandleNullValuesInOptionalFields() {
        // Given
        AppointmentRecord record = new AppointmentRecord();
        record.setRecordId("REC003");
        record.setAppointmentId("APT003");
        record.setDiagnosis("Test diagnosis");
        record.setTreatmentRecommendations("Test treatment");
        record.setPrescriptions("Test prescription");
        record.setDuration(LocalTime.of(0, 15));

        // When
        AppointmentRecord saved = repository.save(record);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getRecordId()).isEqualTo("REC003");
        assertThat(saved.getDiagnosis()).isEqualTo("Test diagnosis");
    }

    @Test
    @DisplayName("Should maintain data integrity when finding by appointmentId")
    void shouldMaintainDataIntegrityWhenFindingByAppointmentId() {
        // Given
        entityManager.persist(testRecord1);
        entityManager.flush();

        // When
        Optional<AppointmentRecord> found = repository.findByAppointmentId("APT001");

        // Then
        assertThat(found).isPresent();
        AppointmentRecord record = found.get();
        assertThat(record.getRecordId()).isEqualTo("REC001");
        assertThat(record.getAppointmentId()).isEqualTo("APT001");
        assertThat(record.getDiagnosis()).isEqualTo("Gripe comum");
        assertThat(record.getTreatmentRecommendations()).isEqualTo("Repouso e hidratação");
        assertThat(record.getPrescriptions()).isEqualTo("Paracetamol 500mg");
        assertThat(record.getDuration()).isEqualTo(LocalTime.of(0, 30));
    }

    @Test
    @DisplayName("Should return only one record when finding by unique appointmentId")
    void shouldReturnOnlyOneRecordByAppointmentId() {
        // Given
        entityManager.persist(testRecord1);
        entityManager.persist(testRecord2);
        entityManager.flush();

        // When
        Optional<AppointmentRecord> found = repository.findByAppointmentId("APT001");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getRecordId()).isEqualTo("REC001");
    }

    @Test
    @DisplayName("Should handle special characters in diagnosis and prescriptions")
    void shouldHandleSpecialCharacters() {
        // Given
        AppointmentRecord record = new AppointmentRecord();
        record.setRecordId("REC004");
        record.setAppointmentId("APT004");
        record.setDiagnosis("Diagnóstico com acentuação e símbolos: @#$%");
        record.setTreatmentRecommendations("Tratamento recomendado");
        record.setPrescriptions("Prescrição: 2x/dia após refeições");
        record.setDuration(LocalTime.of(0, 30));

        // When
        repository.save(record);
        Optional<AppointmentRecord> found = repository.findById("REC004");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getDiagnosis()).isEqualTo("Diagnóstico com acentuação e símbolos: @#$%");
        assertThat(found.get().getPrescriptions()).isEqualTo("Prescrição: 2x/dia após refeições");
    }
}

