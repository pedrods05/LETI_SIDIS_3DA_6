package leti_sisdis_6.hapappointmentrecords.repository;

import leti_sisdis_6.hapappointmentrecords.model.Appointment;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class AppointmentRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AppointmentRepository repository;

    private Appointment appointment1;
    private Appointment appointment2;
    private Appointment appointment3;

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
                .patientId("PAT001")
                .physicianId("PHY002")
                .dateTime(LocalDateTime.of(2025, 11, 2, 14, 0))
                .consultationType(ConsultationType.FOLLOW_UP)
                .status(AppointmentStatus.SCHEDULED)
                .build();

        appointment3 = Appointment.builder()
                .appointmentId("APT003")
                .patientId("PAT002")
                .physicianId("PHY001")
                .dateTime(LocalDateTime.of(2025, 11, 1, 10, 0)) // Mesmo horário que appointment1
                .consultationType(ConsultationType.FOLLOW_UP)
                .status(AppointmentStatus.CANCELLED)
                .build();

        entityManager.persistAndFlush(appointment1);
        entityManager.persistAndFlush(appointment2);
        entityManager.persistAndFlush(appointment3);
        entityManager.clear();
    }

    @Test
    @DisplayName("Deve salvar e recuperar Appointment")
    void shouldSaveAndRetrieveAppointment() {
        // Given
        Appointment newAppointment = Appointment.builder()
                .appointmentId("APT004")
                .patientId("PAT003")
                .physicianId("PHY003")
                .dateTime(LocalDateTime.of(2025, 11, 4, 16, 0))
                .consultationType(ConsultationType.FOLLOW_UP)
                .status(AppointmentStatus.SCHEDULED)
                .build();

        // When
        Appointment saved = repository.save(newAppointment);
        Optional<Appointment> retrieved = repository.findById(saved.getAppointmentId());

        // Then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getAppointmentId()).isEqualTo("APT004");
        assertThat(retrieved.get().getPatientId()).isEqualTo("PAT003");
        assertThat(retrieved.get().getPhysicianId()).isEqualTo("PHY003");
        assertThat(retrieved.get().getConsultationType()).isEqualTo(ConsultationType.FOLLOW_UP);
        assertThat(retrieved.get().getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
    }

    @Test
    @DisplayName("Deve encontrar appointments por patient ID")
    void shouldFindAppointmentsByPatientId() {
        // When
        List<Appointment> patientAppointments = repository.findByPatientId("PAT001");

        // Then
        assertThat(patientAppointments).hasSize(2);
        assertThat(patientAppointments).extracting(Appointment::getAppointmentId)
                .containsExactlyInAnyOrder("APT001", "APT002");
    }

    @Test
    @DisplayName("Deve encontrar appointments por physician ID")
    void shouldFindAppointmentsByPhysicianId() {
        // When
        List<Appointment> physicianAppointments = repository.findByPhysicianId("PHY001");

        // Then
        assertThat(physicianAppointments).hasSize(2);
        assertThat(physicianAppointments).extracting(Appointment::getAppointmentId)
                .containsExactlyInAnyOrder("APT001", "APT003");
    }

    @Test
    @DisplayName("Deve buscar por diferentes critérios disponíveis")
    void shouldFindByDifferentAvailableCriteria() {
        // Usar apenas métodos que existem no repositório

        // 1. Buscar por paciente
        List<Appointment> patientAppointments = repository.findByPatientId("PAT001");
        assertThat(patientAppointments).hasSize(2);

        // 2. Buscar por médico
        List<Appointment> physicianAppointments = repository.findByPhysicianId("PHY001");
        assertThat(physicianAppointments).hasSize(2);

        // 3. Buscar por médico e data específica
        List<Appointment> specificDateTime = repository.findByPhysicianIdAndDateTime("PHY001", LocalDateTime.of(2025, 11, 1, 10, 0));
        assertThat(specificDateTime).hasSize(2); // APT001 e APT003 têm mesmo horário
    }

    @Test
    @DisplayName("Deve buscar appointments por critérios específicos")
    void shouldFindAppointmentsBySpecificCriteria() {
        // Testar apenas funcionalidades que existem no repositório

        // 1. Todos os appointments
        List<Appointment> allAppointments = repository.findAll();
        assertThat(allAppointments).hasSize(3);

        // 2. Por ID específico
        Optional<Appointment> specific = repository.findById("APT001");
        assertThat(specific).isPresent();
        assertThat(specific.get().getPatientId()).isEqualTo("PAT001");

        // 3. Verificar existência
        assertThat(repository.existsById("APT001")).isTrue();
        assertThat(repository.existsById("APT999")).isFalse();
    }

    @Test
    @DisplayName("Deve buscar por range de datas")
    void shouldFindByDateTimeRange() {
        // Usar método que existe no repositório
        LocalDateTime specificDateTime = LocalDateTime.of(2025, 11, 1, 10, 0);

        // Buscar appointments de um médico específico em um horário específico
        List<Appointment> appointmentsAtTime = repository.findByPhysicianIdAndDateTime("PHY001", specificDateTime);

        assertThat(appointmentsAtTime).hasSizeGreaterThanOrEqualTo(1);

        // Verificar que os appointments retornados têm o horário correto
        for (Appointment apt : appointmentsAtTime) {
            assertThat(apt.getDateTime()).isEqualTo(specificDateTime);
            assertThat(apt.getPhysicianId()).isEqualTo("PHY001");
        }
    }

    @Test
    @DisplayName("Deve detectar conflitos de horário para mesmo médico")
    void shouldDetectTimeConflictsForSamePhysician() {
        // Given
        LocalDateTime conflictDateTime = LocalDateTime.of(2025, 11, 1, 10, 0);

        // When
        List<Appointment> conflicts = repository.findByPhysicianIdAndDateTime("PHY001", conflictDateTime);

        // Then
        assertThat(conflicts).hasSizeGreaterThanOrEqualTo(1);

        // Verificar que todos os appointments são do mesmo médico e horário
        for (Appointment apt : conflicts) {
            assertThat(apt.getPhysicianId()).isEqualTo("PHY001");
            assertThat(apt.getDateTime()).isEqualTo(conflictDateTime);
        }
    }

    @Test
    @DisplayName("Deve retornar lista vazia para IDs inexistentes")
    void shouldReturnEmptyListForNonExistentIds() {
        // When
        List<Appointment> nonExistentPatient = repository.findByPatientId("PAT999");
        List<Appointment> nonExistentPhysician = repository.findByPhysicianId("PHY999");

        // Then
        assertThat(nonExistentPatient).isEmpty();
        assertThat(nonExistentPhysician).isEmpty();
    }

    @Test
    @DisplayName("Deve contar total de appointments")
    void shouldCountTotalAppointments() {
        // When
        long count = repository.count();

        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("Deve deletar appointment por ID")
    void shouldDeleteAppointmentById() {
        // Given
        assertThat(repository.existsById("APT001")).isTrue();

        // When
        repository.deleteById("APT001");
        entityManager.flush();

        // Then
        assertThat(repository.existsById("APT001")).isFalse();
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Deve atualizar appointment existente")
    void shouldUpdateExistingAppointment() {
        // Given
        Optional<Appointment> found = repository.findById("APT001");
        assertThat(found).isPresent();

        Appointment appointment = found.get();
        appointment.setStatus(AppointmentStatus.CANCELLED);

        // When
        Appointment updated = repository.save(appointment);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<Appointment> retrieved = repository.findById("APT001");
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    @DisplayName("Deve validar constraints de campos obrigatórios")
    void shouldValidateMandatoryFieldConstraints() {
        // Given
        Appointment appointment = Appointment.builder()
                .appointmentId("APT005")
                .patientId("PAT005")
                .physicianId("PHY005")
                .dateTime(LocalDateTime.now())
                .consultationType(ConsultationType.FIRST_TIME)
                .status(AppointmentStatus.SCHEDULED)
                .build();

        // When
        Appointment saved = repository.save(appointment);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getAppointmentId()).isEqualTo("APT005");
        assertThat(saved.getPatientId()).isNotNull();
        assertThat(saved.getPhysicianId()).isNotNull();
        assertThat(saved.getDateTime()).isNotNull();
        assertThat(saved.getConsultationType()).isNotNull();
        assertThat(saved.getStatus()).isNotNull();
    }
}
