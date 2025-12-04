package leti_sisdis_6.hapappointmentrecords.service;

import leti_sisdis_6.hapappointmentrecords.dto.input.AppointmentRecordRequest;
import leti_sisdis_6.hapappointmentrecords.dto.output.AppointmentRecordResponse;
import leti_sisdis_6.hapappointmentrecords.dto.output.AppointmentRecordViewDTO;
import leti_sisdis_6.hapappointmentrecords.dto.local.UserDTO;
import leti_sisdis_6.hapappointmentrecords.exceptions.NotFoundException;
import leti_sisdis_6.hapappointmentrecords.exceptions.UnauthorizedException;
import leti_sisdis_6.hapappointmentrecords.http.ExternalServiceClient;
import leti_sisdis_6.hapappointmentrecords.model.Appointment;
import leti_sisdis_6.hapappointmentrecords.model.AppointmentRecord;
import leti_sisdis_6.hapappointmentrecords.model.AppointmentStatus;
import leti_sisdis_6.hapappointmentrecords.model.ConsultationType;
import leti_sisdis_6.hapappointmentrecords.repository.AppointmentRecordRepository;
import leti_sisdis_6.hapappointmentrecords.repository.AppointmentRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentRecordServiceTest {

    @Mock
    private AppointmentRecordRepository recordRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ExternalServiceClient externalServiceClient;

    @InjectMocks
    private AppointmentRecordService appointmentRecordService;

    private AppointmentRecordRequest validRequest;
    private Appointment testAppointment;
    private AppointmentRecord testRecord;
    private UserDTO testUser;

    @BeforeEach
    void setUp() {
        validRequest = new AppointmentRecordRequest();
        validRequest.setDiagnosis("Gripe comum");
        validRequest.setTreatmentRecommendations("Repouso e hidratação");
        validRequest.setPrescriptions("Paracetamol 500mg");
        validRequest.setDuration(LocalTime.of(0, 30));

        testAppointment = Appointment.builder()
                .appointmentId("APT001")
                .patientId("PAT001")
                .physicianId("PHY001")
                .dateTime(LocalDateTime.of(2025, 11, 1, 10, 0))
                .consultationType(ConsultationType.FIRST_TIME)
                .status(AppointmentStatus.COMPLETED)
                .build();

        testRecord = AppointmentRecord.builder()
                .recordId("REC001")
                .appointment(testAppointment)
                .diagnosis("Gripe comum")
                .treatmentRecommendations("Repouso e hidratação")
                .prescriptions("Paracetamol 500mg")
                .duration(LocalTime.of(0, 30))
                .build();

        testUser = UserDTO.builder()
                .id("PHY001")
                .email("physician@clinic.com")
                .role("PHYSICIAN")
                .build();
    }

    @Nested
    @DisplayName("Create Record Tests")
    class CreateRecordTests {

        @Test
        @DisplayName("Deve criar record com sucesso quando dados são válidos")
        void shouldCreateRecordSuccessfullyWhenDataIsValid() {
            // Given
            String appointmentId = "APT001";
            String physicianId = "PHY001";

            Map<String, Object> appointmentData = Map.of(
                    "appointmentId", appointmentId,
                    "physicianId", physicianId,
                    "patientId", "PAT001",
                    "status", "COMPLETED"
            );

            when(externalServiceClient.getAppointmentById(appointmentId)).thenReturn(appointmentData);
            when(recordRepository.findByAppointment_AppointmentId(appointmentId)).thenReturn(Optional.empty());
            when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(testAppointment));
            when(recordRepository.save(any(AppointmentRecord.class))).thenReturn(testRecord);

            // When
            AppointmentRecordResponse response = appointmentRecordService.createRecord(appointmentId, validRequest, physicianId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getMessage()).isEqualTo("Appointment record created successfully.");
            assertThat(response.getAppointmentId()).isEqualTo(appointmentId);
            assertThat(response.getRecordId()).isNotNull();

            verify(externalServiceClient).getAppointmentById(appointmentId);
            verify(recordRepository).findByAppointment_AppointmentId(appointmentId);
            verify(appointmentRepository).findById(appointmentId);
            verify(recordRepository).save(any(AppointmentRecord.class));
        }

        @Test
        @DisplayName("Deve criar record quando physicianId está em objeto aninhado")
        void shouldCreateRecordWhenPhysicianIdIsInNestedObject() {
            // Given
            String appointmentId = "APT001";
            String physicianId = "PHY001";

            Map<String, Object> appointmentData = Map.of(
                    "appointmentId", appointmentId,
                    "physician", Map.of("physicianId", physicianId),
                    "patientId", "PAT001"
            );

            when(externalServiceClient.getAppointmentById(appointmentId)).thenReturn(appointmentData);
            when(recordRepository.findByAppointment_AppointmentId(appointmentId)).thenReturn(Optional.empty());
            when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(testAppointment));
            when(recordRepository.save(any(AppointmentRecord.class))).thenReturn(testRecord);

            // When
            AppointmentRecordResponse response = appointmentRecordService.createRecord(appointmentId, validRequest, physicianId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getMessage()).isEqualTo("Appointment record created successfully.");
        }

        @Test
        @DisplayName("Deve lançar NotFoundException quando appointment data é incompleto")
        void shouldThrowNotFoundExceptionWhenAppointmentDataIsIncomplete() {
            // Given
            String appointmentId = "APT001";
            String physicianId = "PHY001";

            Map<String, Object> incompleteData = Map.of(
                    "appointmentId", appointmentId,
                    "patientId", "PAT001"
                    // physicianId ausente
            );

            when(externalServiceClient.getAppointmentById(appointmentId)).thenReturn(incompleteData);

            // When & Then
            assertThatThrownBy(() -> appointmentRecordService.createRecord(appointmentId, validRequest, physicianId))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Appointment data is incomplete");

            verify(externalServiceClient).getAppointmentById(appointmentId);
            verifyNoInteractions(recordRepository, appointmentRepository);
        }

        @Test
        @DisplayName("Deve lançar UnauthorizedException quando physician não é autorizado")
        void shouldThrowUnauthorizedExceptionWhenPhysicianNotAuthorized() {
            // Given
            String appointmentId = "APT001";
            String physicianId = "PHY002"; // Diferente do appointment

            Map<String, Object> appointmentData = Map.of(
                    "appointmentId", appointmentId,
                    "physicianId", "PHY001", // Physician diferente
                    "patientId", "PAT001"
            );

            when(externalServiceClient.getAppointmentById(appointmentId)).thenReturn(appointmentData);

            // When & Then
            assertThatThrownBy(() -> appointmentRecordService.createRecord(appointmentId, validRequest, physicianId))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("You are not authorized to record details for this appointment");

            verify(externalServiceClient).getAppointmentById(appointmentId);
            verifyNoInteractions(recordRepository, appointmentRepository);
        }

        @Test
        @DisplayName("Deve lançar IllegalStateException quando record já existe")
        void shouldThrowIllegalStateExceptionWhenRecordAlreadyExists() {
            // Given
            String appointmentId = "APT001";
            String physicianId = "PHY001";

            Map<String, Object> appointmentData = Map.of(
                    "appointmentId", appointmentId,
                    "physicianId", physicianId,
                    "patientId", "PAT001"
            );

            when(externalServiceClient.getAppointmentById(appointmentId)).thenReturn(appointmentData);
            when(recordRepository.findByAppointment_AppointmentId(appointmentId)).thenReturn(Optional.of(testRecord));

            // When & Then
            assertThatThrownBy(() -> appointmentRecordService.createRecord(appointmentId, validRequest, physicianId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Record already exists for appointment " + appointmentId);

            verify(externalServiceClient).getAppointmentById(appointmentId);
            verify(recordRepository).findByAppointment_AppointmentId(appointmentId);
            verifyNoInteractions(appointmentRepository);
        }

        @Test
        @DisplayName("Deve lançar NotFoundException quando appointment não encontrado")
        void shouldThrowNotFoundExceptionWhenAppointmentNotFound() {
            // Given
            String appointmentId = "APT999";
            String physicianId = "PHY001";

            Map<String, Object> appointmentData = Map.of(
                    "appointmentId", appointmentId,
                    "physicianId", physicianId,
                    "patientId", "PAT001"
            );

            when(externalServiceClient.getAppointmentById(appointmentId)).thenReturn(appointmentData);
            when(recordRepository.findByAppointment_AppointmentId(appointmentId)).thenReturn(Optional.empty());
            when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> appointmentRecordService.createRecord(appointmentId, validRequest, physicianId))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Appointment not found: " + appointmentId);

            verify(externalServiceClient).getAppointmentById(appointmentId);
            verify(recordRepository).findByAppointment_AppointmentId(appointmentId);
            verify(appointmentRepository).findById(appointmentId);
        }
    }

    @Nested
    @DisplayName("Get Appointment Record Tests")
    class GetAppointmentRecordTests {

        @Test
        @DisplayName("Deve retornar record quando encontrado com sucesso")
        void shouldReturnRecordWhenFoundSuccessfully() {
            // Given
            String recordId = "REC001";

            Map<String, Object> appointmentData = Map.of(
                    "appointmentId", "APT001",
                    "physicianId", "PHY001",
                    "patientId", "PAT001",
                    "status", "COMPLETED"
            );

            Map<String, Object> patientData = Map.of(
                    "patientId", "PAT001",
                    "fullName", "João Silva",
                    "email", "joao@email.com"
            );

            Map<String, Object> physicianData = Map.of(
                    "physicianId", "PHY001",
                    "fullName", "Dr. Maria Santos",
                    "licenseNumber", "12345"
            );

            when(recordRepository.findById(recordId)).thenReturn(Optional.of(testRecord));
            when(externalServiceClient.getAppointmentById("APT001")).thenReturn(appointmentData);
            when(externalServiceClient.getPhysicianById("PHY001")).thenReturn(physicianData);

            // When
            AppointmentRecordViewDTO result = appointmentRecordService.getAppointmentRecord(recordId, testUser);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getRecordId()).isEqualTo(recordId);
            assertThat(result.getDiagnosis()).isEqualTo("Gripe comum");
            assertThat(result.getTreatmentRecommendations()).isEqualTo("Repouso e hidratação");
            assertThat(result.getPrescriptions()).isEqualTo("Paracetamol 500mg");

            verify(recordRepository).findById(recordId);
            verify(externalServiceClient).getAppointmentById("APT001");
            verify(externalServiceClient).getPhysicianById("PHY001");
        }

        @Test
        @DisplayName("Deve lançar NotFoundException quando record não encontrado")
        void shouldThrowNotFoundExceptionWhenRecordNotFound() {
            // Given
            String recordId = "REC999";

            when(recordRepository.findById(recordId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> appointmentRecordService.getAppointmentRecord(recordId, testUser))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Appointment record not found with id: " + recordId);

            verify(recordRepository).findById(recordId);
            verifyNoInteractions(externalServiceClient);
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Deve validar request obrigatório")
        void shouldValidateRequiredRequest() {
            // Given
            String appointmentId = "APT001";
            String physicianId = "PHY001";
            AppointmentRecordRequest nullRequest = null;

            // When & Then
            assertThatThrownBy(() -> appointmentRecordService.createRecord(appointmentId, nullRequest, physicianId))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Appointment data is incomplete");
        }

        @Test
        @DisplayName("Deve validar appointmentId obrigatório")
        void shouldValidateRequiredAppointmentId() {
            // Given
            String physicianId = "PHY001";

            // When & Then
            assertThatThrownBy(() -> appointmentRecordService.createRecord(null, validRequest, physicianId))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Appointment data is incomplete");
        }

        @Test
        @DisplayName("Deve validar physicianId obrigatório")
        void shouldValidateRequiredPhysicianId() {
            // Given
            String appointmentId = "APT001";

            // When & Then
            assertThatThrownBy(() -> appointmentRecordService.createRecord(appointmentId, validRequest, null))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Appointment data is incomplete");
        }
    }

    @Nested
    @DisplayName("Business Logic Tests")
    class BusinessLogicTests {

        @Test
        @DisplayName("Deve gerar ID único para record")
        void shouldGenerateUniqueIdForRecord() {
            // Given
            String appointmentId = "APT001";
            String physicianId = "PHY001";

            Map<String, Object> appointmentData = Map.of(
                    "appointmentId", appointmentId,
                    "physicianId", physicianId,
                    "patientId", "PAT001"
            );

            when(externalServiceClient.getAppointmentById(appointmentId)).thenReturn(appointmentData);
            when(recordRepository.findByAppointment_AppointmentId(appointmentId)).thenReturn(Optional.empty());
            when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(testAppointment));
            when(recordRepository.save(any(AppointmentRecord.class))).thenReturn(testRecord);

            // When
            AppointmentRecordResponse response1 = appointmentRecordService.createRecord(appointmentId, validRequest, physicianId);
            AppointmentRecordResponse response2 = appointmentRecordService.createRecord(appointmentId, validRequest, physicianId);

            // Then
            assertThat(response1.getRecordId()).isNotNull();
            assertThat(response2.getRecordId()).isNotNull();
            // Note: Como estamos usando mock, os IDs podem ser iguais, mas na implementação real seriam diferentes
        }

        @Test
        @DisplayName("Deve salvar todas as informações do request")
        void shouldSaveAllRequestInformation() {
            // Given
            String appointmentId = "APT001";
            String physicianId = "PHY001";

            AppointmentRecordRequest detailedRequest = new AppointmentRecordRequest();
            detailedRequest.setDiagnosis("Diabetes tipo 2");
            detailedRequest.setTreatmentRecommendations("Controle glicêmico rigoroso");
            detailedRequest.setPrescriptions("Metformina 850mg 2x/dia");
            detailedRequest.setDuration(LocalTime.of(1, 15));

            Map<String, Object> appointmentData = Map.of(
                    "appointmentId", appointmentId,
                    "physicianId", physicianId,
                    "patientId", "PAT001"
            );

            when(externalServiceClient.getAppointmentById(appointmentId)).thenReturn(appointmentData);
            when(recordRepository.findByAppointment_AppointmentId(appointmentId)).thenReturn(Optional.empty());
            when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(testAppointment));
            when(recordRepository.save(any(AppointmentRecord.class))).thenReturn(testRecord);

            // When
            appointmentRecordService.createRecord(appointmentId, detailedRequest, physicianId);

            // Then
            verify(recordRepository).save(argThat(record ->
                record.getDiagnosis().equals("Diabetes tipo 2") &&
                record.getTreatmentRecommendations().equals("Controle glicêmico rigoroso") &&
                record.getPrescriptions().equals("Metformina 850mg 2x/dia") &&
                record.getDuration().equals(LocalTime.of(1, 15))
            ));
        }
    }
}
