package leti_sisdis_6.happhysicians.services;

import leti_sisdis_6.happhysicians.api.AppointmentMapper;
import leti_sisdis_6.happhysicians.dto.input.ScheduleAppointmentRequest;
import leti_sisdis_6.happhysicians.dto.input.UpdateAppointmentRequest;
import leti_sisdis_6.happhysicians.dto.output.AppointmentDetailsDTO;
import leti_sisdis_6.happhysicians.exceptions.MicroserviceCommunicationException;
import leti_sisdis_6.happhysicians.exceptions.PatientNotFoundException;
import leti_sisdis_6.happhysicians.model.Appointment;
import leti_sisdis_6.happhysicians.model.AppointmentStatus;
import leti_sisdis_6.happhysicians.model.ConsultationType;
import leti_sisdis_6.happhysicians.model.Department;
import leti_sisdis_6.happhysicians.model.Physician;
import leti_sisdis_6.happhysicians.model.Specialty;
import leti_sisdis_6.happhysicians.repository.AppointmentRepository;
import leti_sisdis_6.happhysicians.repository.PhysicianRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PhysicianRepository physicianRepository;

    @Mock
    private ExternalServiceClient externalServiceClient;

    @Mock
    private AppointmentMapper appointmentMapper;

    @InjectMocks
    private AppointmentService appointmentService;

    private Physician testPhysician;
    private Appointment testAppointment;
    private ScheduleAppointmentRequest scheduleRequest;

    @BeforeEach
    void setUp() {
        // Manually inject @Autowired fields that Mockito can't inject
        ReflectionTestUtils.setField(appointmentService, "appointmentRepository", appointmentRepository);
        ReflectionTestUtils.setField(appointmentService, "physicianRepository", physicianRepository);
        ReflectionTestUtils.setField(appointmentService, "externalServiceClient", externalServiceClient);
        
        Department department = Department.builder()
                .departmentId("DEPT01")
                .code("CARD")
                .name("Cardiology")
                .build();

        Specialty specialty = Specialty.builder()
                .specialtyId("SPEC01")
                .name("Cardiologist")
                .build();

        testPhysician = Physician.builder()
                .physicianId("PHY01")
                .fullName("Dr. John Doe")
                .licenseNumber("LIC123")
                .username("john.doe@hospital.com")
                .password("encodedPassword")
                .specialty(specialty)
                .department(department)
                .workingHourStart(LocalTime.of(9, 0))
                .workingHourEnd(LocalTime.of(17, 0))
                .build();

        LocalDateTime appointmentDateTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        testAppointment = Appointment.builder()
                .appointmentId("APT01")
                .patientId("PAT01")
                .physician(testPhysician)
                .dateTime(appointmentDateTime)
                .consultationType(ConsultationType.FIRST_TIME)
                .status(AppointmentStatus.SCHEDULED)
                .wasRescheduled(false)
                .build();

        scheduleRequest = new ScheduleAppointmentRequest();
        scheduleRequest.setAppointmentId("APT01");
        scheduleRequest.setPatientId("PAT01");
        scheduleRequest.setPhysicianId("PHY01");
        scheduleRequest.setDateTime(appointmentDateTime);
        scheduleRequest.setConsultationType(ConsultationType.FIRST_TIME);
        scheduleRequest.setStatus(AppointmentStatus.SCHEDULED);
        scheduleRequest.setWasRescheduled(false);
    }

    @Test
    void testCreateAppointment_Success() {
        // Arrange
        when(physicianRepository.findById("PHY01")).thenReturn(Optional.of(testPhysician));
        Map<String, Object> patientData = new HashMap<>();
        patientData.put("fullName", "John Patient");
        patientData.put("email", "patient@example.com");
        patientData.put("phoneNumber", "123456789");
        
        when(externalServiceClient.getPatientById("PAT01")).thenReturn(patientData);
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(testAppointment);
        Map<String, Object> createdResponse = new HashMap<>();
        createdResponse.put("appointmentId", "APT01");
        when(externalServiceClient.createAppointmentInRecords(any())).thenReturn(createdResponse);

        // Act
        Appointment result = appointmentService.createAppointment(scheduleRequest);

        // Assert
        assertNotNull(result);
        assertEquals("APT01", result.getAppointmentId());
        verify(externalServiceClient, times(1)).createAppointmentInRecords(any());
        verify(appointmentRepository, times(1)).save(any(Appointment.class));
    }

    @Test
    void testCreateAppointment_PhysicianNotFound() {
        // Arrange
        when(physicianRepository.findById("PHY01")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            appointmentService.createAppointment(scheduleRequest);
        });
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    void testCreateAppointment_MicroserviceCommunicationException() {
        // Arrange
        when(physicianRepository.findById("PHY01")).thenReturn(Optional.of(testPhysician));
        doThrow(new MicroserviceCommunicationException("AppointmentRecords", "createAppointment", "Conflict")).when(externalServiceClient)
                .createAppointmentInRecords(any());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            appointmentService.createAppointment(scheduleRequest);
        });
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    void testGetAllAppointments_Success() {
        // Arrange
        Map<String, Object> remoteAppointment = new HashMap<>();
        remoteAppointment.put("appointmentId", "APT01");
        remoteAppointment.put("patientId", "PAT01");
        remoteAppointment.put("physicianId", "PHY01");
        remoteAppointment.put("dateTime", LocalDateTime.now().toString());
        remoteAppointment.put("consultationType", "FIRST_TIME");
        remoteAppointment.put("status", "SCHEDULED");

        when(externalServiceClient.listAppointments()).thenReturn(Collections.singletonList(remoteAppointment));
        when(physicianRepository.findById("PHY01")).thenReturn(Optional.of(testPhysician));

        // Act
        List<Appointment> result = appointmentService.getAllAppointments();

        // Assert
        assertNotNull(result);
        verify(externalServiceClient, times(1)).listAppointments();
    }

    @Test
    void testGetAppointmentsByPhysician_Success() {
        // Arrange
        Map<String, Object> remoteAppointment = new HashMap<>();
        remoteAppointment.put("appointmentId", "APT01");
        remoteAppointment.put("patientId", "PAT01");
        remoteAppointment.put("physicianId", "PHY01");
        remoteAppointment.put("dateTime", LocalDateTime.now().toString());
        remoteAppointment.put("consultationType", "FIRST_TIME");
        remoteAppointment.put("status", "SCHEDULED");

        when(externalServiceClient.listAppointmentsByPhysician("PHY01"))
                .thenReturn(Collections.singletonList(remoteAppointment));
        when(physicianRepository.findById("PHY01")).thenReturn(Optional.of(testPhysician));

        // Act
        List<Appointment> result = appointmentService.getAppointmentsByPhysician("PHY01");

        // Assert
        assertNotNull(result);
        verify(externalServiceClient, times(1)).listAppointmentsByPhysician("PHY01");
    }

    @Test
    void testGetAppointmentById_Success() {
        // Arrange
        String appointmentId = "APT01";
        Map<String, Object> remoteAppointment = new HashMap<>();
        remoteAppointment.put("appointmentId", appointmentId);
        remoteAppointment.put("patientId", "PAT01");
        remoteAppointment.put("physicianId", "PHY01");
        remoteAppointment.put("dateTime", LocalDateTime.now().toString());
        remoteAppointment.put("consultationType", "FIRST_TIME");
        remoteAppointment.put("status", "SCHEDULED");

        when(externalServiceClient.getAppointment(appointmentId)).thenReturn(remoteAppointment);
        when(physicianRepository.findById("PHY01")).thenReturn(Optional.of(testPhysician));

        // Act
        Optional<Appointment> result = appointmentService.getAppointmentById(appointmentId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(appointmentId, result.get().getAppointmentId());
    }

    @Test
    void testGetAppointmentById_FromLocalRepository() {
        // Arrange
        String appointmentId = "APT01";
        when(externalServiceClient.getAppointment(appointmentId))
                .thenThrow(new RuntimeException("Not found"));
        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(testAppointment));

        // Act
        Optional<Appointment> result = appointmentService.getAppointmentById(appointmentId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(appointmentId, result.get().getAppointmentId());
    }

    @Test
    void testUpdateAppointment_Success() {
        // Arrange
        String appointmentId = "APT01";
        UpdateAppointmentRequest updateRequest = new UpdateAppointmentRequest();
        updateRequest.setDateTime(LocalDateTime.now().plusDays(2));
        updateRequest.setStatus(AppointmentStatus.SCHEDULED);

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(testAppointment));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(testAppointment);
        Map<String, Object> updatedResponse = new HashMap<>();
        updatedResponse.put("appointmentId", appointmentId);
        when(externalServiceClient.updateAppointmentInRecords(anyString(), any())).thenReturn(updatedResponse);

        // Act
        Appointment result = appointmentService.updateAppointment(appointmentId, updateRequest);

        // Assert
        assertNotNull(result);
        verify(externalServiceClient, times(1)).updateAppointmentInRecords(anyString(), any());
        verify(appointmentRepository, times(1)).save(testAppointment);
    }

    @Test
    void testUpdateAppointment_NotFound() {
        // Arrange
        String appointmentId = "APT99";
        UpdateAppointmentRequest updateRequest = new UpdateAppointmentRequest();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

        // Act
        Appointment result = appointmentService.updateAppointment(appointmentId, updateRequest);

        // Assert
        assertNull(result);
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    void testDeleteAppointment_Success() {
        // Arrange
        String appointmentId = "APT01";
        when(appointmentRepository.existsById(appointmentId)).thenReturn(true);
        doNothing().when(externalServiceClient).deleteAppointmentInRecords(appointmentId);
        doNothing().when(appointmentRepository).deleteById(appointmentId);

        // Act
        boolean result = appointmentService.deleteAppointment(appointmentId);

        // Assert
        assertTrue(result);
        verify(externalServiceClient, times(1)).deleteAppointmentInRecords(appointmentId);
        verify(appointmentRepository, times(1)).deleteById(appointmentId);
    }

    @Test
    void testDeleteAppointment_NotFound() {
        // Arrange
        String appointmentId = "APT99";
        when(appointmentRepository.existsById(appointmentId)).thenReturn(false);

        // Act
        boolean result = appointmentService.deleteAppointment(appointmentId);

        // Assert
        assertFalse(result);
        verify(externalServiceClient, never()).deleteAppointmentInRecords(anyString());
        verify(appointmentRepository, never()).deleteById(anyString());
    }

    @Test
    void testGetAppointmentWithPatient_Success() {
        // Arrange
        String appointmentId = "APT01";
        Map<String, Object> patientData = new HashMap<>();
        patientData.put("fullName", "John Patient");
        patientData.put("email", "patient@example.com");
        patientData.put("phoneNumber", "123456789");

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(testAppointment));
        when(externalServiceClient.getPatientById("PAT01")).thenReturn(patientData);

        // Act
        AppointmentDetailsDTO result = appointmentService.getAppointmentWithPatient(appointmentId);

        // Assert
        assertNotNull(result);
        verify(externalServiceClient, times(1)).getPatientById("PAT01");
    }

    @Test
    void testGetAppointmentWithPatient_PatientNotFound() {
        // Arrange
        String appointmentId = "APT01";
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(testAppointment));
        when(externalServiceClient.getPatientById("PAT01"))
                .thenThrow(new PatientNotFoundException("Patient not found"));

        // Act
        AppointmentDetailsDTO result = appointmentService.getAppointmentWithPatient(appointmentId);

        // Assert
        assertNotNull(result);
    }
}

