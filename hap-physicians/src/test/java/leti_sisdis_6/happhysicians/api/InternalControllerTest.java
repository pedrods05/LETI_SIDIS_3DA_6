package leti_sisdis_6.happhysicians.api;

import leti_sisdis_6.happhysicians.dto.output.AppointmentDetailsDTO;
import leti_sisdis_6.happhysicians.model.Appointment;
import leti_sisdis_6.happhysicians.model.AppointmentStatus;
import leti_sisdis_6.happhysicians.model.ConsultationType;
import leti_sisdis_6.happhysicians.model.Department;
import leti_sisdis_6.happhysicians.model.Physician;
import leti_sisdis_6.happhysicians.model.Specialty;
import leti_sisdis_6.happhysicians.repository.AppointmentRepository;
import leti_sisdis_6.happhysicians.repository.PhysicianRepository;
import leti_sisdis_6.happhysicians.services.AppointmentService;
import leti_sisdis_6.happhysicians.services.ExternalServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternalControllerTest {

    @Mock
    private PhysicianRepository physicianRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ExternalServiceClient externalServiceClient;

    @Mock
    private AppointmentService appointmentService;

    @InjectMocks
    private InternalController internalController;

    private Physician testPhysician;
    private Appointment testAppointment;

    @BeforeEach
    void setUp() {
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
    }

    @Test
    void testGetPhysicianInternal_Success() {
        // Arrange
        String physicianId = "PHY01";
        when(physicianRepository.findById(physicianId)).thenReturn(Optional.of(testPhysician));

        // Act
        ResponseEntity<Physician> response = internalController.getPhysicianInternal(physicianId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(physicianId, response.getBody().getPhysicianId());
        verify(physicianRepository, times(1)).findById(physicianId);
    }

    @Test
    void testGetPhysicianInternal_NotFound() {
        // Arrange
        String physicianId = "PHY99";
        when(physicianRepository.findById(physicianId)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<Physician> response = internalController.getPhysicianInternal(physicianId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testGetAllPhysiciansInternal_Success() {
        // Arrange
        List<Physician> physicians = Arrays.asList(testPhysician);
        when(physicianRepository.findAll()).thenReturn(physicians);

        // Act
        List<Physician> result = internalController.getAllPhysiciansInternal();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(physicianRepository, times(1)).findAll();
    }

    @Test
    void testGetAppointmentInternal_Success() {
        // Arrange
        String appointmentId = "APT01";
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(testAppointment));

        // Act
        ResponseEntity<Appointment> response = internalController.getAppointmentInternal(appointmentId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(appointmentId, response.getBody().getAppointmentId());
        verify(appointmentRepository, times(1)).findById(appointmentId);
    }

    @Test
    void testGetAppointmentInternal_NotFound() {
        // Arrange
        String appointmentId = "APT99";
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<Appointment> response = internalController.getAppointmentInternal(appointmentId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testGetAllAppointmentsInternal_Success() {
        // Arrange
        List<Appointment> appointments = Arrays.asList(testAppointment);
        when(appointmentRepository.findAll()).thenReturn(appointments);

        // Act
        List<Appointment> result = internalController.getAllAppointmentsInternal();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(appointmentRepository, times(1)).findAll();
    }

    @Test
    void testCreatePhysicianInternal_Success() {
        // Arrange
        when(physicianRepository.save(any(Physician.class))).thenReturn(testPhysician);

        // Act
        Physician result = internalController.createPhysicianInternal(testPhysician);

        // Assert
        assertNotNull(result);
        assertEquals("PHY01", result.getPhysicianId());
        verify(physicianRepository, times(1)).save(testPhysician);
    }

    @Test
    void testGetAppointmentWithPatientInternal_Success() {
        // Arrange
        String appointmentId = "APT01";
        AppointmentDetailsDTO details = new AppointmentDetailsDTO(testAppointment);
        when(appointmentService.getAppointmentWithPatient(appointmentId)).thenReturn(details);

        // Act
        ResponseEntity<AppointmentDetailsDTO> response = 
                internalController.getAppointmentWithPatientInternal(appointmentId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(appointmentService, times(1)).getAppointmentWithPatient(appointmentId);
    }

    @Test
    void testGetAppointmentWithPatientInternal_NotFound() {
        // Arrange
        String appointmentId = "APT99";
        when(appointmentService.getAppointmentWithPatient(appointmentId))
                .thenThrow(new RuntimeException("Not found"));

        // Act
        ResponseEntity<AppointmentDetailsDTO> response = 
                internalController.getAppointmentWithPatientInternal(appointmentId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testGetPeers_Success() {
        // Arrange
        List<String> peers = Arrays.asList("http://peer1:8080", "http://peer2:8080");
        when(externalServiceClient.getPeerUrls()).thenReturn(peers);

        // Act
        List<String> result = internalController.getPeers();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(externalServiceClient, times(1)).getPeerUrls();
    }

    @Test
    void testGetPeerStatus_Success() {
        // Arrange
        when(externalServiceClient.getCurrentInstanceUrl()).thenReturn("http://localhost:8081");
        when(externalServiceClient.getPeerCount()).thenReturn(2);
        when(externalServiceClient.hasPeers()).thenReturn(true);
        when(externalServiceClient.getPeerUrls()).thenReturn(Arrays.asList("http://peer1:8080"));

        // Act
        Map<String, Object> status = internalController.getPeerStatus();

        // Assert
        assertNotNull(status);
        assertTrue(status.containsKey("currentInstance"));
        assertTrue(status.containsKey("totalPeers"));
        assertTrue(status.containsKey("hasPeers"));
        assertTrue(status.containsKey("peerUrls"));
        assertEquals(2, status.get("totalPeers"));
        assertTrue((Boolean) status.get("hasPeers"));
    }
}

