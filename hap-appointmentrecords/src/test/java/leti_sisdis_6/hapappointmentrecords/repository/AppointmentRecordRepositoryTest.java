package leti_sisdis_6.hapappointmentrecords.repository;

import leti_sisdis_6.hapappointmentrecords.model.Appointment;
import leti_sisdis_6.hapappointmentrecords.model.AppointmentRecord;
import leti_sisdis_6.hapappointmentrecords.model.AppointmentStatus;
import leti_sisdis_6.hapappointmentrecords.model.ConsultationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
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

    private Appointment appointment1;
    private Appointment appointment2;
    private AppointmentRecord record1;
    private AppointmentRecord record2;

    @BeforeEach
    void setUp() {
        appointment1 = Appointment.builder()
                .appointmentId("APT001")
                .patientId("PAT001")
                .physicianId("PHY001")
                .dateTime(LocalDateTime.of(2025, 11, 1, 10, 0))
                .consultationType(ConsultationType.FIRST_TIME)
                .status(AppointmentStatus.COMPLETED)
                .build();

        appointment2 = Appointment.builder()
                .appointmentId("APT002")
                .patientId("PAT002")
                .physicianId("PHY001")
                .dateTime(LocalDateTime.of(2025, 11, 2, 14, 0))
                .consultationType(ConsultationType.FOLLOW_UP)
                .status(AppointmentStatus.COMPLETED)
                .build();

        record1 = AppointmentRecord.builder()
                .recordId("REC001")
                .appointment(appointment1)
                .diagnosis("Gripe comum")
                .treatmentRecommendations("Repouso e hidratação")
                .prescriptions("Paracetamol 500mg")
                .duration(LocalTime.of(0, 30))
                .build();

        record2 = AppointmentRecord.builder()
                .recordId("REC002")
                .appointment(appointment2)
                .diagnosis("Pressão alta")
                .treatmentRecommendations("Dieta com baixo teor de sódio")
                .prescriptions("Losartana 50mg")
                .duration(LocalTime.of(0, 45))
                .build();

        // Persist appointments first
        entityManager.persistAndFlush(appointment1);
        entityManager.persistAndFlush(appointment2);

        // Then persist records
        entityManager.persistAndFlush(record1);
        entityManager.persistAndFlush(record2);

        entityManager.clear();
    }

    @Test
    @DisplayName("Deve salvar e recuperar AppointmentRecord")
    void shouldSaveAndRetrieveAppointmentRecord() {
        // Given
        Appointment validAppointment = Appointment.builder()
                .appointmentId("APT-ERROR-001")
                .patientId("PAT-ERROR-001")
                .physicianId("PHY001")
                .dateTime(LocalDateTime.of(2025, 11, 15, 16, 0))
                .consultationType(ConsultationType.FOLLOW_UP)
                .status(AppointmentStatus.COMPLETED)
                .build();

        entityManager.persistAndFlush(validAppointment);

        AppointmentRecord newRecord = AppointmentRecord.builder()
                .recordId("REC003")
                .appointment(validAppointment)
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
        assertThat(retrieved.get().getAppointment().getAppointmentId()).isEqualTo("APT-ERROR-001");
    }

    @Test
    @DisplayName("Deve encontrar record por appointment ID")
    void shouldFindRecordByAppointmentId() {
        // When
        Optional<AppointmentRecord> record = repository.findByAppointment_AppointmentId("APT001");

        // Then
        assertThat(record).isPresent();
        assertThat(record.get().getRecordId()).isEqualTo("REC001");
        assertThat(record.get().getDiagnosis()).isEqualTo("Gripe comum");
    }

    @Test
    @DisplayName("Deve retornar empty para appointment ID inexistente")
    void shouldReturnEmptyForNonExistentAppointmentId() {
        // When
        Optional<AppointmentRecord> record = repository.findByAppointment_AppointmentId("APT999");

        // Then
        assertThat(record).isEmpty();
    }

    @Test
    @DisplayName("Deve buscar records por múltiplos appointment IDs")
    void shouldFindRecordsByMultipleAppointmentIds() {
        // When
        Optional<AppointmentRecord> record1 = repository.findByAppointment_AppointmentId("APT001");
        Optional<AppointmentRecord> record2 = repository.findByAppointment_AppointmentId("APT002");

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
        Appointment validAppointment = Appointment.builder()
                .appointmentId("APT-ERROR-001")
                .patientId("PAT-ERROR-001")
                .physicianId("PHY001")
                .dateTime(LocalDateTime.of(2025, 11, 15, 16, 0))
                .consultationType(ConsultationType.FOLLOW_UP)
                .status(AppointmentStatus.COMPLETED)
                .build();

        entityManager.persistAndFlush(validAppointment);

        AppointmentRecord record = AppointmentRecord.builder()
                .recordId("REC-CONSTRAINTS")
                .appointment(validAppointment)
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
        assertThat(saved.getAppointment()).isNotNull();
        assertThat(saved.getDiagnosis()).isNotNull();
    }
}
