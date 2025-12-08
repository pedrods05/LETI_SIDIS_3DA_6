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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentRecordServiceTest {

    @Mock
    private AppointmentRecordRepository recordRepository;

    @Mock
    private ExternalServiceClient externalServiceClient;

    @Mock
    private AppointmentEventsPublisher eventsPublisher;

    @Mock
    private AppointmentRecordProjectionRepository recordProjectionRepository;

    @InjectMocks
    private AppointmentRecordService service;

    @Test
    @DisplayName("Create Record - Success Flow")
    void createRecord_Success() {
        String appointmentId = "app-123";
        String physicianId = "phys-001";
        String patientId = "pat-001";

        AppointmentRecordRequest request = new AppointmentRecordRequest();
        request.setDiagnosis("Flu");
        request.setTreatmentRecommendations("Rest");

        Map<String, Object> appointmentData = Map.of(
                "physicianId", physicianId,
                "patientId", patientId
        );
        when(externalServiceClient.getAppointmentById(appointmentId)).thenReturn(appointmentData);

        when(recordRepository.findByAppointmentId(appointmentId)).thenReturn(Optional.empty());

        AppointmentRecordResponse response = service.createRecord(appointmentId, request, physicianId);


        assertNotNull(response);
        assertEquals(appointmentId, response.getAppointmentId());
        assertTrue(response.getRecordId().startsWith("REC"));

        verify(recordRepository).save(any(AppointmentRecord.class));

        ArgumentCaptor<AppointmentRecordProjection> projCaptor = ArgumentCaptor.forClass(AppointmentRecordProjection.class);
        verify(recordProjectionRepository).save(projCaptor.capture());
        AppointmentRecordProjection savedProj = projCaptor.getValue();
        assertEquals(patientId, savedProj.getPatientId());
        assertEquals("Flu", savedProj.getDiagnosis());

        verify(eventsPublisher).publishAppointmentRecordCreated(any(AppointmentRecordCreatedEvent.class));
    }

    @Test
    @DisplayName("Create Record - Extract Nested Physician ID")
    void createRecord_NestedPhysicianId() {
        String appointmentId = "app-123";
        String physicianId = "phys-nested";

        Map<String, Object> nestedPhysician = Map.of("physicianId", physicianId);
        Map<String, Object> appointmentData = Map.of(
                "physician", nestedPhysician,
                "patientId", "pat-1"
        );

        when(externalServiceClient.getAppointmentById(appointmentId)).thenReturn(appointmentData);
        when(recordRepository.findByAppointmentId(appointmentId)).thenReturn(Optional.empty());

        AppointmentRecordRequest request = new AppointmentRecordRequest();

        AppointmentRecordResponse response = service.createRecord(appointmentId, request, physicianId);

        assertNotNull(response);
        verify(recordRepository).save(any());
    }

    @Test
    @DisplayName("Create Record - Unauthorized (Wrong Physician)")
    void createRecord_Unauthorized() {
        String appointmentId = "app-123";
        Map<String, Object> appointmentData = Map.of("physicianId", "phys-A"); // Appointment assigned to A

        when(externalServiceClient.getAppointmentById(appointmentId)).thenReturn(appointmentData);

        AppointmentRecordRequest request = new AppointmentRecordRequest();

        assertThrows(UnauthorizedException.class, () ->
                service.createRecord(appointmentId, request, "phys-B")
        );

        verify(recordRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create Record - Duplicate Record")
    void createRecord_Duplicate() {

        String appointmentId = "app-123";
        Map<String, Object> appointmentData = Map.of(
                "physicianId", "phys-1",
                "patientId", "pat-1"
        );

        when(externalServiceClient.getAppointmentById(appointmentId)).thenReturn(appointmentData);

        when(recordRepository.findByAppointmentId(appointmentId)).thenReturn(Optional.of(new AppointmentRecord()));

        AppointmentRecordRequest request = new AppointmentRecordRequest();

        assertThrows(IllegalStateException.class, () ->
                service.createRecord(appointmentId, request, "phys-1")
        );
    }

    @Test
    @DisplayName("Create Record - Incomplete Data (No Patient ID)")
    void createRecord_IncompleteData() {

        String appointmentId = "app-123";

        Map<String, Object> appointmentData = Map.of("physicianId", "phys-1");

        when(externalServiceClient.getAppointmentById(appointmentId)).thenReturn(appointmentData);
        when(recordRepository.findByAppointmentId(appointmentId)).thenReturn(Optional.empty());

        AppointmentRecordRequest request = new AppointmentRecordRequest();

        assertThrows(NotFoundException.class, () ->
                service.createRecord(appointmentId, request, "phys-1")
        );
    }

    @Test
    @DisplayName("Get Record - Success with Physician Enrichment")
    void getAppointmentRecord_Success() {
        String recordId = "rec-1";
        AppointmentRecordProjection projection = AppointmentRecordProjection.builder()
                .recordId(recordId)
                .patientId("pat-1")
                .physicianId("phys-1")
                .diagnosis("Cold")
                .build();

        when(recordProjectionRepository.findById(recordId)).thenReturn(Optional.of(projection));

        when(externalServiceClient.getPhysicianById("phys-1")).thenReturn(Map.of("fullName", "Dr. House"));

        UserDTO adminUser = UserDTO.builder().id("admin").role("ADMIN").build();

        AppointmentRecordViewDTO result = service.getAppointmentRecord(recordId, adminUser);

        assertEquals("Cold", result.getDiagnosis());
        assertEquals("Dr. House", result.getPhysicianName());
    }

    @Test
    @DisplayName("Get Record - Patient Unauthorized")
    void getAppointmentRecord_PatientUnauthorized() {
        String recordId = "rec-1";
        AppointmentRecordProjection projection = AppointmentRecordProjection.builder()
                .recordId(recordId)
                .patientId("pat-1")
                .build();

        when(recordProjectionRepository.findById(recordId)).thenReturn(Optional.of(projection));

        UserDTO otherPatient = UserDTO.builder().id("pat-2").role("PATIENT").build();

        assertThrows(UnauthorizedException.class, () ->
                service.getAppointmentRecord(recordId, otherPatient)
        );
    }

    @Test
    @DisplayName("Get Record - Enrichment Failure Graceful Handling")
    void getAppointmentRecord_EnrichmentFails() {
        AppointmentRecordProjection projection = AppointmentRecordProjection.builder()
                .recordId("rec-1")
                .physicianId("phys-down")
                .build();

        when(recordProjectionRepository.findById("rec-1")).thenReturn(Optional.of(projection));

        when(externalServiceClient.getPhysicianById("phys-down")).thenThrow(new RuntimeException("Service Unavailable"));

        UserDTO admin = UserDTO.builder().role("ADMIN").build();

        AppointmentRecordViewDTO result = service.getAppointmentRecord("rec-1", admin);

        assertEquals("Unknown Physician", result.getPhysicianName()); // Fallback value
    }

    @Test
    @DisplayName("Get Patient Records - Returns List")
    void getPatientRecords_Success() {
        AppointmentRecordProjection p1 = AppointmentRecordProjection.builder().recordId("r1").patientId("pat-1").build();
        AppointmentRecordProjection p2 = AppointmentRecordProjection.builder().recordId("r2").patientId("pat-1").build();

        when(recordProjectionRepository.findByPatientId("pat-1")).thenReturn(List.of(p1, p2));

        List<AppointmentRecordViewDTO> results = service.getPatientRecords("pat-1");

        assertEquals(2, results.size());
        verify(recordProjectionRepository).findByPatientId("pat-1");
    }

    @Test
    @DisplayName("Get Patient Records - Empty List")
    void getPatientRecords_Empty() {
        when(recordProjectionRepository.findByPatientId("pat-1")).thenReturn(Collections.emptyList());

        List<AppointmentRecordViewDTO> results = service.getPatientRecords("pat-1");

        assertTrue(results.isEmpty());
    }
}