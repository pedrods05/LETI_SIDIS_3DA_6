package leti_sisdis_6.hapappointmentrecords.service;

import leti_sisdis_6.hapappointmentrecords.dto.input.AppointmentRecordRequest;
import leti_sisdis_6.hapappointmentrecords.dto.output.AppointmentRecordResponse;
import leti_sisdis_6.hapappointmentrecords.dto.output.AppointmentRecordViewDTO;
import leti_sisdis_6.hapappointmentrecords.dto.local.UserDTO;
import leti_sisdis_6.hapappointmentrecords.exceptions.NotFoundException;
import leti_sisdis_6.hapappointmentrecords.exceptions.UnauthorizedException;
import leti_sisdis_6.hapappointmentrecords.http.ExternalServiceClient;
import leti_sisdis_6.hapappointmentrecords.model.AppointmentRecord;
import leti_sisdis_6.hapappointmentrecords.model.AppointmentRecordProjection;
import leti_sisdis_6.hapappointmentrecords.repository.AppointmentRecordProjectionRepository;
import leti_sisdis_6.hapappointmentrecords.repository.AppointmentRecordRepository;
import leti_sisdis_6.hapappointmentrecords.service.event.AppointmentEventsPublisher;
import leti_sisdis_6.hapappointmentrecords.service.event.AppointmentRecordCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentRecordServiceTest {

    @Mock
    private AppointmentRecordRepository recordRepository;

    @Mock
    private AppointmentRecordProjectionRepository recordProjectionRepository;

    @Mock
    private ExternalServiceClient externalServiceClient;

    @Mock
    private AppointmentEventsPublisher eventsPublisher;

    private AppointmentRecordService service;

    @BeforeEach
    void setUp() {
        service = new AppointmentRecordService(
                recordRepository,
                externalServiceClient,
                eventsPublisher,
                recordProjectionRepository
        );
    }

    @Test
    @DisplayName("Should create appointment record successfully")
    void shouldCreateAppointmentRecordSuccessfully() {
        // Given
        String appointmentId = "APT001";
        String physicianId = "PHY001";
        String patientId = "PAT001";

        AppointmentRecordRequest request = new AppointmentRecordRequest();
        request.setDiagnosis("Gripe comum");
        request.setTreatmentRecommendations("Repouso e hidratação");
        request.setPrescriptions("Paracetamol 500mg");
        request.setDuration(LocalTime.of(0, 30));

        Map<String, Object> appointmentData = new HashMap<>();
        appointmentData.put("physicianId", physicianId);
        appointmentData.put("patientId", patientId);

        when(externalServiceClient.getAppointmentById(appointmentId)).thenReturn(appointmentData);
        when(recordRepository.findByAppointmentId(appointmentId)).thenReturn(Optional.empty());
        when(recordRepository.save(any(AppointmentRecord.class))).thenAnswer(i -> i.getArgument(0));
        when(recordProjectionRepository.save(any(AppointmentRecordProjection.class))).thenAnswer(i -> i.getArgument(0));

        // When
        AppointmentRecordResponse response = service.createRecord(appointmentId, request, physicianId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAppointmentId()).isEqualTo(appointmentId);
        assertThat(response.getRecordId()).isNotNull();
        assertThat(response.getMessage()).contains("successfully");

        verify(recordRepository).save(any(AppointmentRecord.class));
        verify(recordProjectionRepository).save(any(AppointmentRecordProjection.class));
        verify(eventsPublisher).publishAppointmentRecordCreated(any(AppointmentRecordCreatedEvent.class));
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when physician does not match")
    void shouldThrowUnauthorizedExceptionWhenPhysicianDoesNotMatch() {
        // Given
        String appointmentId = "APT001";
        String requestingPhysicianId = "PHY001";
        String appointmentPhysicianId = "PHY002";

        AppointmentRecordRequest request = new AppointmentRecordRequest();
        request.setDiagnosis("Test");

        Map<String, Object> appointmentData = new HashMap<>();
        appointmentData.put("physicianId", appointmentPhysicianId);
        appointmentData.put("patientId", "PAT001");

        when(externalServiceClient.getAppointmentById(appointmentId)).thenReturn(appointmentData);

        // When/Then
        assertThatThrownBy(() -> service.createRecord(appointmentId, request, requestingPhysicianId))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("not authorized");

        verify(recordRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when record already exists")
    void shouldThrowIllegalStateExceptionWhenRecordAlreadyExists() {
        // Given
        String appointmentId = "APT001";
        String physicianId = "PHY001";

        AppointmentRecordRequest request = new AppointmentRecordRequest();
        request.setDiagnosis("Test");

        Map<String, Object> appointmentData = new HashMap<>();
        appointmentData.put("physicianId", physicianId);
        appointmentData.put("patientId", "PAT001");

        AppointmentRecord existingRecord = new AppointmentRecord();
        existingRecord.setRecordId("REC001");

        when(externalServiceClient.getAppointmentById(appointmentId)).thenReturn(appointmentData);
        when(recordRepository.findByAppointmentId(appointmentId)).thenReturn(Optional.of(existingRecord));

        // When/Then
        assertThatThrownBy(() -> service.createRecord(appointmentId, request, physicianId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already exists");

        verify(recordRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw NotFoundException when appointment data is incomplete")
    void shouldThrowNotFoundExceptionWhenAppointmentDataIncomplete() {
        // Given
        String appointmentId = "APT001";
        String physicianId = "PHY001";

        AppointmentRecordRequest request = new AppointmentRecordRequest();
        request.setDiagnosis("Test");

        Map<String, Object> appointmentData = new HashMap<>();
        // Missing physicianId and patientId

        when(externalServiceClient.getAppointmentById(appointmentId)).thenReturn(appointmentData);

        // When/Then
        assertThatThrownBy(() -> service.createRecord(appointmentId, request, physicianId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Should extract physicianId from nested physician object")
    void shouldExtractPhysicianIdFromNestedObject() {
        // Given
        String appointmentId = "APT001";
        String physicianId = "PHY001";

        AppointmentRecordRequest request = new AppointmentRecordRequest();
        request.setDiagnosis("Test");

        Map<String, Object> physicianMap = new HashMap<>();
        physicianMap.put("physicianId", physicianId);

        Map<String, Object> appointmentData = new HashMap<>();
        appointmentData.put("physician", physicianMap);
        appointmentData.put("patientId", "PAT001");

        when(externalServiceClient.getAppointmentById(appointmentId)).thenReturn(appointmentData);
        when(recordRepository.findByAppointmentId(appointmentId)).thenReturn(Optional.empty());
        when(recordRepository.save(any(AppointmentRecord.class))).thenAnswer(i -> i.getArgument(0));
        when(recordProjectionRepository.save(any(AppointmentRecordProjection.class))).thenAnswer(i -> i.getArgument(0));

        // When
        AppointmentRecordResponse response = service.createRecord(appointmentId, request, physicianId);

        // Then
        assertThat(response).isNotNull();
        verify(recordRepository).save(any(AppointmentRecord.class));
    }

    @Test
    @DisplayName("Should get appointment record successfully")
    void shouldGetAppointmentRecordSuccessfully() {
        // Given
        String recordId = "REC001";
        UserDTO currentUser = new UserDTO();
        currentUser.setId("PHY001");
        currentUser.setRole("PHYSICIAN");

        AppointmentRecordProjection projection = new AppointmentRecordProjection();
        projection.setRecordId(recordId);
        projection.setAppointmentId("APT001");
        projection.setPatientId("PAT001");
        projection.setPhysicianId("PHY001");
        projection.setDiagnosis("Gripe comum");
        projection.setDuration(LocalTime.of(0, 30));

        Map<String, Object> physicianData = new HashMap<>();
        physicianData.put("fullName", "Dr. Silva");

        when(recordProjectionRepository.findById(recordId)).thenReturn(Optional.of(projection));
        when(externalServiceClient.getPhysicianById("PHY001")).thenReturn(physicianData);

        // When
        AppointmentRecordViewDTO result = service.getAppointmentRecord(recordId, currentUser);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRecordId()).isEqualTo(recordId);
        assertThat(result.getPhysicianName()).isEqualTo("Dr. Silva");
        assertThat(result.getDiagnosis()).isEqualTo("Gripe comum");
    }

    @Test
    @DisplayName("Should throw NotFoundException when record not found")
    void shouldThrowNotFoundExceptionWhenRecordNotFound() {
        // Given
        String recordId = "NON_EXISTENT";
        UserDTO currentUser = new UserDTO();
        currentUser.setRole("PHYSICIAN");

        when(recordProjectionRepository.findById(recordId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> service.getAppointmentRecord(recordId, currentUser))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when patient tries to access other patient's record")
    void shouldThrowUnauthorizedExceptionForPatientAccessingOtherRecord() {
        // Given
        String recordId = "REC001";
        UserDTO currentUser = new UserDTO();
        currentUser.setId("PAT002");
        currentUser.setRole("PATIENT");

        AppointmentRecordProjection projection = new AppointmentRecordProjection();
        projection.setRecordId(recordId);
        projection.setPatientId("PAT001"); // Different patient

        when(recordProjectionRepository.findById(recordId)).thenReturn(Optional.of(projection));

        // When/Then
        assertThatThrownBy(() -> service.getAppointmentRecord(recordId, currentUser))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("not authorized");
    }

    @Test
    @DisplayName("Should allow patient to access their own record")
    void shouldAllowPatientToAccessOwnRecord() {
        // Given
        String recordId = "REC001";
        UserDTO currentUser = new UserDTO();
        currentUser.setId("PAT001");
        currentUser.setRole("PATIENT");

        AppointmentRecordProjection projection = new AppointmentRecordProjection();
        projection.setRecordId(recordId);
        projection.setPatientId("PAT001"); // Same patient
        projection.setPhysicianId("PHY001");
        projection.setDiagnosis("Test");

        when(recordProjectionRepository.findById(recordId)).thenReturn(Optional.of(projection));
        when(externalServiceClient.getPhysicianById(anyString())).thenReturn(Map.of("fullName", "Dr. Test"));

        // When
        AppointmentRecordViewDTO result = service.getAppointmentRecord(recordId, currentUser);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRecordId()).isEqualTo(recordId);
    }

    @Test
    @DisplayName("Should handle physician service failure gracefully")
    void shouldHandlePhysicianServiceFailureGracefully() {
        // Given
        String recordId = "REC001";
        UserDTO currentUser = new UserDTO();
        currentUser.setRole("PHYSICIAN");

        AppointmentRecordProjection projection = new AppointmentRecordProjection();
        projection.setRecordId(recordId);
        projection.setPhysicianId("PHY001");
        projection.setDiagnosis("Test");

        when(recordProjectionRepository.findById(recordId)).thenReturn(Optional.of(projection));
        when(externalServiceClient.getPhysicianById("PHY001")).thenThrow(new RuntimeException("Service unavailable"));

        // When
        AppointmentRecordViewDTO result = service.getAppointmentRecord(recordId, currentUser);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPhysicianName()).isEqualTo("Unknown Physician");
    }

    @Test
    @DisplayName("Should get patient records successfully")
    void shouldGetPatientRecordsSuccessfully() {
        // Given
        String patientId = "PAT001";

        AppointmentRecordProjection projection1 = new AppointmentRecordProjection();
        projection1.setRecordId("REC001");
        projection1.setPatientId(patientId);
        projection1.setPhysicianId("PHY001");
        projection1.setDiagnosis("Gripe");

        AppointmentRecordProjection projection2 = new AppointmentRecordProjection();
        projection2.setRecordId("REC002");
        projection2.setPatientId(patientId);
        projection2.setPhysicianId("PHY002");
        projection2.setDiagnosis("Check-up");

        when(recordProjectionRepository.findByPatientId(patientId))
                .thenReturn(Arrays.asList(projection1, projection2));
        when(externalServiceClient.getPhysicianById(anyString()))
                .thenReturn(Map.of("fullName", "Dr. Test"));

        // When
        List<AppointmentRecordViewDTO> results = service.getPatientRecords(patientId);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).extracting(AppointmentRecordViewDTO::getRecordId)
                .containsExactly("REC001", "REC002");
    }

    @Test
    @DisplayName("Should return empty list when patient has no records")
    void shouldReturnEmptyListWhenPatientHasNoRecords() {
        // Given
        String patientId = "PAT001";
        when(recordProjectionRepository.findByPatientId(patientId)).thenReturn(Collections.emptyList());

        // When
        List<AppointmentRecordViewDTO> results = service.getPatientRecords(patientId);

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should generate unique record IDs")
    void shouldGenerateUniqueRecordIds() {
        // Given
        String appointmentId = "APT001";
        String physicianId = "PHY001";

        AppointmentRecordRequest request = new AppointmentRecordRequest();
        request.setDiagnosis("Test");

        Map<String, Object> appointmentData = new HashMap<>();
        appointmentData.put("physicianId", physicianId);
        appointmentData.put("patientId", "PAT001");

        when(externalServiceClient.getAppointmentById(appointmentId)).thenReturn(appointmentData);
        when(recordRepository.findByAppointmentId(appointmentId)).thenReturn(Optional.empty());
        when(recordRepository.save(any(AppointmentRecord.class))).thenAnswer(i -> i.getArgument(0));
        when(recordProjectionRepository.save(any(AppointmentRecordProjection.class))).thenAnswer(i -> i.getArgument(0));

        // When
        AppointmentRecordResponse response1 = service.createRecord(appointmentId, request, physicianId);
        AppointmentRecordResponse response2 = service.createRecord(appointmentId, request, physicianId);

        // Then
        assertThat(response1.getRecordId()).isNotEqualTo(response2.getRecordId());
        assertThat(response1.getRecordId()).startsWith("REC");
        assertThat(response2.getRecordId()).startsWith("REC");
    }

    @Test
    @DisplayName("Should save both write and read models")
    void shouldSaveBothWriteAndReadModels() {
        // Given
        String appointmentId = "APT001";
        String physicianId = "PHY001";

        AppointmentRecordRequest request = new AppointmentRecordRequest();
        request.setDiagnosis("Test diagnosis");
        request.setTreatmentRecommendations("Test treatment");
        request.setPrescriptions("Test prescription");
        request.setDuration(LocalTime.of(0, 30));

        Map<String, Object> appointmentData = new HashMap<>();
        appointmentData.put("physicianId", physicianId);
        appointmentData.put("patientId", "PAT001");

        when(externalServiceClient.getAppointmentById(appointmentId)).thenReturn(appointmentData);
        when(recordRepository.findByAppointmentId(appointmentId)).thenReturn(Optional.empty());
        when(recordRepository.save(any(AppointmentRecord.class))).thenAnswer(i -> i.getArgument(0));
        when(recordProjectionRepository.save(any(AppointmentRecordProjection.class))).thenAnswer(i -> i.getArgument(0));

        ArgumentCaptor<AppointmentRecord> recordCaptor = ArgumentCaptor.forClass(AppointmentRecord.class);
        ArgumentCaptor<AppointmentRecordProjection> projectionCaptor = ArgumentCaptor.forClass(AppointmentRecordProjection.class);

        // When
        service.createRecord(appointmentId, request, physicianId);

        // Then
        verify(recordRepository).save(recordCaptor.capture());
        verify(recordProjectionRepository).save(projectionCaptor.capture());

        AppointmentRecord savedRecord = recordCaptor.getValue();
        AppointmentRecordProjection savedProjection = projectionCaptor.getValue();

        assertThat(savedRecord.getDiagnosis()).isEqualTo("Test diagnosis");
        assertThat(savedProjection.getDiagnosis()).isEqualTo("Test diagnosis");
        assertThat(savedRecord.getRecordId()).isEqualTo(savedProjection.getRecordId());
    }
}

