package leti_sisdis_6.hapappointmentrecords.setup;

import leti_sisdis_6.hapappointmentrecords.model.Appointment;
import leti_sisdis_6.hapappointmentrecords.model.AppointmentRecord;
import leti_sisdis_6.hapappointmentrecords.repository.AppointmentRecordRepository;
import leti_sisdis_6.hapappointmentrecords.repository.AppointmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataBootstrapTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private AppointmentRecordRepository appointmentRecordRepository;

    @InjectMocks
    private DataBootstrap dataBootstrap;

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(appointmentRepository, appointmentRecordRepository);
    }

    @Test
    @DisplayName("Deve criar dados de bootstrap quando não existem appointments")
    void shouldCreateBootstrapDataWhenNoAppointmentsExist() throws Exception {
        // Given
        when(appointmentRepository.count()).thenReturn(0L);

        // When
        dataBootstrap.run();

        // Then
        verify(appointmentRepository).count();
        verify(appointmentRepository).saveAll(any());
        verify(appointmentRecordRepository).saveAll(any());
    }

    @Test
    @DisplayName("Não deve criar dados se appointments já existem")
    void shouldNotCreateDataIfAppointmentsAlreadyExist() throws Exception {
        // Given
        when(appointmentRepository.count()).thenReturn(5L);

        // When
        dataBootstrap.run();

        // Then
        verify(appointmentRepository).count();
        verify(appointmentRepository, never()).saveAll(any());
        verify(appointmentRecordRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Deve criar múltiplos appointments")
    void shouldCreateMultipleAppointments() throws Exception {
        // Given
        when(appointmentRepository.count()).thenReturn(0L);
        ArgumentCaptor<List<Appointment>> appointmentsCaptor = ArgumentCaptor.forClass(List.class);

        // When
        dataBootstrap.run();

        // Then
        verify(appointmentRepository).saveAll(appointmentsCaptor.capture());

        List<Appointment> appointments = appointmentsCaptor.getValue();
        assertThat(appointments).isNotEmpty();
        assertThat(appointments).hasSizeGreaterThan(1);

        // Verificar que todos os appointments têm IDs únicos
        List<String> appointmentIds = appointments.stream()
                .map(Appointment::getAppointmentId)
                .toList();

        assertThat(appointmentIds).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("Deve criar appointment records correspondentes")
    void shouldCreateCorrespondingAppointmentRecords() throws Exception {
        // Given
        when(appointmentRepository.count()).thenReturn(0L);
        ArgumentCaptor<List<AppointmentRecord>> recordsCaptor = ArgumentCaptor.forClass(List.class);

        // When
        dataBootstrap.run();

        // Then
        verify(appointmentRecordRepository).saveAll(recordsCaptor.capture());

        List<AppointmentRecord> records = recordsCaptor.getValue();
        assertThat(records).isNotEmpty();

        // Verificar que todos os records têm IDs únicos
        List<String> recordIds = records.stream()
                .map(AppointmentRecord::getRecordId)
                .toList();

        assertThat(recordIds).doesNotHaveDuplicates();

        // Verificar que todos os records têm diagnósticos
        assertThat(records).allMatch(record ->
            record.getDiagnosis() != null && !record.getDiagnosis().isBlank()
        );
    }

    @Test
    @DisplayName("Deve criar appointments com diferentes status")
    void shouldCreateAppointmentsWithDifferentStatuses() throws Exception {
        // Given
        when(appointmentRepository.count()).thenReturn(0L);
        ArgumentCaptor<List<Appointment>> appointmentsCaptor = ArgumentCaptor.forClass(List.class);

        // When
        dataBootstrap.run();

        // Then
        verify(appointmentRepository).saveAll(appointmentsCaptor.capture());

        List<Appointment> appointments = appointmentsCaptor.getValue();

        // Verificar que há appointments com diferentes status
        boolean hasCompleted = appointments.stream()
                .anyMatch(apt -> apt.getStatus().name().equals("COMPLETED"));

        assertThat(hasCompleted).isTrue();
    }

    @Test
    @DisplayName("Deve criar appointments com diferentes tipos de consulta")
    void shouldCreateAppointmentsWithDifferentConsultationTypes() throws Exception {
        // Given
        when(appointmentRepository.count()).thenReturn(0L);
        ArgumentCaptor<List<Appointment>> appointmentsCaptor = ArgumentCaptor.forClass(List.class);

        // When
        dataBootstrap.run();

        // Then
        verify(appointmentRepository).saveAll(appointmentsCaptor.capture());

        List<Appointment> appointments = appointmentsCaptor.getValue();

        // Verificar que há appointments com diferentes tipos
        boolean hasFirstTime = appointments.stream()
                .anyMatch(apt -> apt.getConsultationType().name().equals("FIRST_TIME"));

        boolean hasFollowUp = appointments.stream()
                .anyMatch(apt -> apt.getConsultationType().name().equals("FOLLOW_UP"));

        assertThat(hasFirstTime).isTrue();
        assertThat(hasFollowUp).isTrue();
    }

    @Test
    @DisplayName("Deve criar appointments com diferentes horários")
    void shouldCreateAppointmentsWithDifferentTimes() throws Exception {
        // Given
        when(appointmentRepository.count()).thenReturn(0L);
        ArgumentCaptor<List<Appointment>> appointmentsCaptor = ArgumentCaptor.forClass(List.class);

        // When
        dataBootstrap.run();

        // Then
        verify(appointmentRepository).saveAll(appointmentsCaptor.capture());

        List<Appointment> appointments = appointmentsCaptor.getValue();

        // Verificar que há appointments em horários diferentes
        List<LocalDateTime> uniqueTimes = appointments.stream()
                .map(Appointment::getDateTime)
                .distinct()
                .toList();

        assertThat(uniqueTimes).hasSizeGreaterThan(1);
    }

    @Test
    @DisplayName("Deve validar integridade dos dados criados")
    void shouldValidateDataIntegrity() throws Exception {
        // Given
        when(appointmentRepository.count()).thenReturn(0L);
        ArgumentCaptor<List<Appointment>> appointmentsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<AppointmentRecord>> recordsCaptor = ArgumentCaptor.forClass(List.class);

        // When
        dataBootstrap.run();

        // Then
        verify(appointmentRepository).saveAll(appointmentsCaptor.capture());
        verify(appointmentRecordRepository).saveAll(recordsCaptor.capture());

        List<Appointment> appointments = appointmentsCaptor.getValue();
        List<AppointmentRecord> records = recordsCaptor.getValue();

        // Verificar que todos os appointments têm campos obrigatórios
        assertThat(appointments).allMatch(apt ->
            apt.getAppointmentId() != null &&
            apt.getPatientId() != null &&
            apt.getPhysicianId() != null &&
            apt.getDateTime() != null &&
            apt.getConsultationType() != null &&
            apt.getStatus() != null
        );

        // Verificar que todos os records têm campos obrigatórios
        assertThat(records).allMatch(record ->
            record.getRecordId() != null &&
            record.getAppointment() != null
        );
    }
}
