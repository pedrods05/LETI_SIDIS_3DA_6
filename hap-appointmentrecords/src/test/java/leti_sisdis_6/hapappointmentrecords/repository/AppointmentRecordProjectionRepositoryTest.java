package leti_sisdis_6.hapappointmentrecords.repository;

import leti_sisdis_6.hapappointmentrecords.model.AppointmentRecordProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@TestPropertySource(properties = {
        "spring.data.mongodb.uri=mongodb://localhost:27017/test-appointmentrecords-projection"
})
class AppointmentRecordProjectionRepositoryTest {

    @Autowired
    private AppointmentRecordProjectionRepository repository;

    private AppointmentRecordProjection testProjection1;
    private AppointmentRecordProjection testProjection2;
    private AppointmentRecordProjection testProjection3;

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        testProjection1 = new AppointmentRecordProjection();
        testProjection1.setRecordId("REC001");
        testProjection1.setAppointmentId("APT001");
        testProjection1.setPatientId("PAT001");
        testProjection1.setPhysicianId("PHY001");
        testProjection1.setDiagnosis("Gripe comum");
        testProjection1.setTreatmentRecommendations("Repouso e hidratação");
        testProjection1.setPrescriptions("Paracetamol 500mg");
        testProjection1.setDuration(LocalTime.of(0, 30));

        testProjection2 = new AppointmentRecordProjection();
        testProjection2.setRecordId("REC002");
        testProjection2.setAppointmentId("APT002");
        testProjection2.setPatientId("PAT001");
        testProjection2.setPhysicianId("PHY002");
        testProjection2.setDiagnosis("Hipertensão");
        testProjection2.setTreatmentRecommendations("Dieta e exercício");
        testProjection2.setPrescriptions("Enalapril 10mg");
        testProjection2.setDuration(LocalTime.of(0, 45));

        testProjection3 = new AppointmentRecordProjection();
        testProjection3.setRecordId("REC003");
        testProjection3.setAppointmentId("APT003");
        testProjection3.setPatientId("PAT002");
        testProjection3.setPhysicianId("PHY001");
        testProjection3.setDiagnosis("Diabetes tipo 2");
        testProjection3.setTreatmentRecommendations("Controle de açúcar");
        testProjection3.setPrescriptions("Metformina 850mg");
        testProjection3.setDuration(LocalTime.of(1, 0));
    }

    @Test
    @DisplayName("Should save appointment record projection successfully")
    void shouldSaveAppointmentRecordProjection() {
        // When
        AppointmentRecordProjection saved = repository.save(testProjection1);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getRecordId()).isEqualTo("REC001");
        assertThat(saved.getAppointmentId()).isEqualTo("APT001");
        assertThat(saved.getPhysicianId()).isEqualTo("PHY001");
    }

    @Test
    @DisplayName("Should find appointment record projection by ID")
    void shouldFindAppointmentRecordProjectionById() {
        // Given
        repository.save(testProjection1);

        // When
        Optional<AppointmentRecordProjection> found = repository.findById("REC001");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getRecordId()).isEqualTo("REC001");
        assertThat(found.get().getDiagnosis()).isEqualTo("Gripe comum");
    }

    @Test
    @DisplayName("Should return empty when projection not found by ID")
    void shouldReturnEmptyWhenNotFoundById() {
        // When
        Optional<AppointmentRecordProjection> found = repository.findById("NON_EXISTENT");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find all projections by patientId")
    void shouldFindAllProjectionsByPatientId() {
        // Given
        repository.save(testProjection1);
        repository.save(testProjection2);
        repository.save(testProjection3);

        // When
        List<AppointmentRecordProjection> found = repository.findByPatientId("PAT001");

        // Then
        assertThat(found).hasSize(2);
        assertThat(found).extracting(AppointmentRecordProjection::getRecordId)
                .containsExactlyInAnyOrder("REC001", "REC002");
        assertThat(found).allMatch(p -> p.getPatientId().equals("PAT001"));
    }

    @Test
    @DisplayName("Should return empty list when no projections found for patientId")
    void shouldReturnEmptyListWhenNoProjectionsFoundForPatientId() {
        // Given
        repository.save(testProjection1);

        // When
        List<AppointmentRecordProjection> found = repository.findByPatientId("NON_EXISTENT");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find all appointment record projections")
    void shouldFindAllAppointmentRecordProjections() {
        // Given
        repository.save(testProjection1);
        repository.save(testProjection2);
        repository.save(testProjection3);

        // When
        List<AppointmentRecordProjection> all = repository.findAll();

        // Then
        assertThat(all).hasSize(3);
        assertThat(all).extracting(AppointmentRecordProjection::getRecordId)
                .containsExactlyInAnyOrder("REC001", "REC002", "REC003");
    }

    @Test
    @DisplayName("Should delete appointment record projection")
    void shouldDeleteAppointmentRecordProjection() {
        // Given
        repository.save(testProjection1);

        // When
        repository.deleteById("REC001");

        // Then
        Optional<AppointmentRecordProjection> found = repository.findById("REC001");
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should update appointment record projection")
    void shouldUpdateAppointmentRecordProjection() {
        // Given
        repository.save(testProjection1);

        // When
        testProjection1.setDiagnosis("Gripe comum - Atualizado");
        testProjection1.setPrescriptions("Paracetamol 500mg + Vitamina C");
        AppointmentRecordProjection updated = repository.save(testProjection1);

        // Then
        Optional<AppointmentRecordProjection> found = repository.findById("REC001");
        assertThat(found).isPresent();
        assertThat(found.get().getDiagnosis()).isEqualTo("Gripe comum - Atualizado");
        assertThat(found.get().getPrescriptions()).isEqualTo("Paracetamol 500mg + Vitamina C");
    }

    @Test
    @DisplayName("Should count appointment record projections")
    void shouldCountAppointmentRecordProjections() {
        // Given
        repository.save(testProjection1);
        repository.save(testProjection2);
        repository.save(testProjection3);

        // When
        long count = repository.count();

        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("Should check if projection exists by ID")
    void shouldCheckIfExistsById() {
        // Given
        repository.save(testProjection1);

        // When/Then
        assertThat(repository.existsById("REC001")).isTrue();
        assertThat(repository.existsById("NON_EXISTENT")).isFalse();
    }

    @Test
    @DisplayName("Should delete all appointment record projections")
    void shouldDeleteAllAppointmentRecordProjections() {
        // Given
        repository.save(testProjection1);
        repository.save(testProjection2);
        repository.save(testProjection3);

        // When
        repository.deleteAll();

        // Then
        assertThat(repository.count()).isZero();
    }

    @Test
    @DisplayName("Should maintain data integrity when finding by patientId")
    void shouldMaintainDataIntegrityWhenFindingByPatientId() {
        // Given
        repository.save(testProjection1);
        repository.save(testProjection2);

        // When
        List<AppointmentRecordProjection> found = repository.findByPatientId("PAT001");

        // Then
        assertThat(found).hasSize(2);

        AppointmentRecordProjection first = found.stream()
                .filter(p -> p.getRecordId().equals("REC001"))
                .findFirst()
                .orElseThrow();

        assertThat(first.getAppointmentId()).isEqualTo("APT001");
        assertThat(first.getPhysicianId()).isEqualTo("PHY001");
        assertThat(first.getDiagnosis()).isEqualTo("Gripe comum");
        assertThat(first.getDuration()).isEqualTo(LocalTime.of(0, 30));
    }

    @Test
    @DisplayName("Should return projections sorted by patientId")
    void shouldReturnProjectionsByPatientId() {
        // Given
        repository.save(testProjection1);
        repository.save(testProjection2);
        repository.save(testProjection3);

        // When
        List<AppointmentRecordProjection> patient1Records = repository.findByPatientId("PAT001");
        List<AppointmentRecordProjection> patient2Records = repository.findByPatientId("PAT002");

        // Then
        assertThat(patient1Records).hasSize(2);
        assertThat(patient2Records).hasSize(1);
        assertThat(patient2Records.get(0).getRecordId()).isEqualTo("REC003");
    }

    @Test
    @DisplayName("Should handle null values in optional fields")
    void shouldHandleNullValuesInOptionalFields() {
        // Given
        AppointmentRecordProjection projection = new AppointmentRecordProjection();
        projection.setRecordId("REC004");
        projection.setAppointmentId("APT004");
        projection.setPatientId("PAT004");
        projection.setPhysicianId("PHY004");
        projection.setDiagnosis("Test diagnosis");
        // Not setting optional fields

        // When
        AppointmentRecordProjection saved = repository.save(projection);

        // Then
        Optional<AppointmentRecordProjection> found = repository.findById("REC004");
        assertThat(found).isPresent();
        assertThat(found.get().getTreatmentRecommendations()).isNull();
        assertThat(found.get().getPrescriptions()).isNull();
    }

    @Test
    @DisplayName("Should handle special characters in fields")
    void shouldHandleSpecialCharacters() {
        // Given
        AppointmentRecordProjection projection = new AppointmentRecordProjection();
        projection.setRecordId("REC005");
        projection.setAppointmentId("APT005");
        projection.setPatientId("PAT005");
        projection.setPhysicianId("PHY005");
        projection.setDiagnosis("Diagnóstico com acentuação e símbolos: @#$%");
        projection.setPrescriptions("Prescrição: 2x/dia após refeições");

        // When
        repository.save(projection);
        Optional<AppointmentRecordProjection> found = repository.findById("REC005");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getDiagnosis()).isEqualTo("Diagnóstico com acentuação e símbolos: @#$%");
    }

    @Test
    @DisplayName("Should handle multiple records for same patient")
    void shouldHandleMultipleRecordsForSamePatient() {
        // Given
        AppointmentRecordProjection record1 = new AppointmentRecordProjection();
        record1.setRecordId("REC010");
        record1.setAppointmentId("APT010");
        record1.setPatientId("PAT100");
        record1.setPhysicianId("PHY001");
        record1.setDiagnosis("Consulta 1");

        AppointmentRecordProjection record2 = new AppointmentRecordProjection();
        record2.setRecordId("REC011");
        record2.setAppointmentId("APT011");
        record2.setPatientId("PAT100");
        record2.setPhysicianId("PHY002");
        record2.setDiagnosis("Consulta 2");

        AppointmentRecordProjection record3 = new AppointmentRecordProjection();
        record3.setRecordId("REC012");
        record3.setAppointmentId("APT012");
        record3.setPatientId("PAT100");
        record3.setPhysicianId("PHY001");
        record3.setDiagnosis("Consulta 3");

        repository.save(record1);
        repository.save(record2);
        repository.save(record3);

        // When
        List<AppointmentRecordProjection> found = repository.findByPatientId("PAT100");

        // Then
        assertThat(found).hasSize(3);
        assertThat(found).extracting(AppointmentRecordProjection::getDiagnosis)
                .containsExactlyInAnyOrder("Consulta 1", "Consulta 2", "Consulta 3");
    }

    @Test
    @DisplayName("Should handle long text in diagnosis and prescriptions")
    void shouldHandleLongText() {
        // Given
        String longDiagnosis = "This is a very long diagnosis text ".repeat(50);
        String longPrescription = "This is a very long prescription text ".repeat(50);

        AppointmentRecordProjection projection = new AppointmentRecordProjection();
        projection.setRecordId("REC006");
        projection.setAppointmentId("APT006");
        projection.setPatientId("PAT006");
        projection.setPhysicianId("PHY006");
        projection.setDiagnosis(longDiagnosis);
        projection.setPrescriptions(longPrescription);

        // When
        repository.save(projection);
        Optional<AppointmentRecordProjection> found = repository.findById("REC006");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getDiagnosis()).hasSize(longDiagnosis.length());
        assertThat(found.get().getPrescriptions()).hasSize(longPrescription.length());
    }
}

