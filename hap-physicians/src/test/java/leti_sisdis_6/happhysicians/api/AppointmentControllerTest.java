package leti_sisdis_6.happhysicians.api;

import leti_sisdis_6.happhysicians.dto.input.ScheduleAppointmentRequest;
import leti_sisdis_6.happhysicians.dto.input.UpdateAppointmentRequest;
import leti_sisdis_6.happhysicians.dto.output.AppointmentDetailsDTO;
import leti_sisdis_6.happhysicians.model.Appointment;
import leti_sisdis_6.happhysicians.model.AppointmentStatus;
import leti_sisdis_6.happhysicians.model.ConsultationType;
import leti_sisdis_6.happhysicians.model.Department;
import leti_sisdis_6.happhysicians.model.Physician;
import leti_sisdis_6.happhysicians.model.Specialty;
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
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentControllerTest {

    @Mock
    private AppointmentService appointmentService;

    @Mock
    private ExternalServiceClient externalServiceClient;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AppointmentController appointmentController;

    private Appointment testAppointment;
    private Physician testPhysician;
    private ScheduleAppointmentRequest scheduleRequest;

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

        scheduleRequest = new ScheduleAppointmentRequest();
        scheduleRequest.setAppointmentId("APT01");
        scheduleRequest.setPatientId("PAT01");
        scheduleRequest.setPhysicianId("PHY01");
        scheduleRequest.setDateTime(appointmentDateTime);
        scheduleRequest.setConsultationType(ConsultationType.FIRST_TIME);
        scheduleRequest.setStatus(AppointmentStatus.SCHEDULED);
    }

    @Test
    void testGetAppointment_Success() {
        // Arrange
        String appointmentId = "APT01";
        when(appointmentService.getAppointmentById(appointmentId))
                .thenReturn(Optional.of(testAppointment));

        // Act
        ResponseEntity<Appointment> response = appointmentController.getAppointment(appointmentId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(appointmentId, response.getBody().getAppointmentId());
        verify(appointmentService, times(1)).getAppointmentById(appointmentId);
    }

    @Test
    void testGetAppointment_NotFound() {
        // Arrange
        String appointmentId = "APT99";
        when(appointmentService.getAppointmentById(appointmentId))
                .thenReturn(Optional.empty());
        when(externalServiceClient.getPeerUrls()).thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<Appointment> response = appointmentController.getAppointment(appointmentId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testGetAppointment_FromPeer() {
        // Arrange
        String appointmentId = "APT01";
        String peerUrl = "http://peer1:8080";
        Appointment peerAppointment = Appointment.builder()
                .appointmentId(appointmentId)
                .patientId("PAT01")
                .build();

        when(appointmentService.getAppointmentById(appointmentId))
                .thenReturn(Optional.empty());
        when(externalServiceClient.getPeerUrls()).thenReturn(Collections.singletonList(peerUrl));
        when(restTemplate.getForObject(eq(peerUrl + "/internal/appointments/" + appointmentId), eq(Appointment.class)))
                .thenReturn(peerAppointment);

        // Act
        ResponseEntity<Appointment> response = appointmentController.getAppointment(appointmentId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(appointmentId, response.getBody().getAppointmentId());
    }

    @Test
    void testCreateAppointment_Success() {
        // Arrange
        when(appointmentService.createAppointment(any(ScheduleAppointmentRequest.class)))
                .thenReturn(testAppointment);

        // Act
        ResponseEntity<?> response = appointmentController.createAppointment(scheduleRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(appointmentService, times(1)).createAppointment(scheduleRequest);
    }

    @Test
    void testCreateAppointment_BadRequest() {
        // Arrange
        when(appointmentService.createAppointment(any(ScheduleAppointmentRequest.class)))
                .thenThrow(new RuntimeException("Invalid appointment"));

        // Act
        ResponseEntity<?> response = appointmentController.createAppointment(scheduleRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testGetAllAppointments_Success() {
        // Arrange
        List<Appointment> appointments = Arrays.asList(testAppointment);
        when(appointmentService.getAllAppointments()).thenReturn(appointments);

        // Act
        List<Appointment> result = appointmentController.getAllAppointments();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(appointmentService, times(1)).getAllAppointments();
    }

    @Test
    void testGetAppointmentsByPhysician_Success() {
        // Arrange
        String physicianId = "PHY01";
        List<Appointment> appointments = Arrays.asList(testAppointment);
        when(appointmentService.getAppointmentsByPhysician(physicianId)).thenReturn(appointments);

        // Act
        List<Appointment> result = appointmentController.getAppointmentsByPhysician(physicianId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(appointmentService, times(1)).getAppointmentsByPhysician(physicianId);
    }

    @Test
    void testGetAppointmentsByPatient_Success() {
        // Arrange
        String patientId = "PAT01";
        List<Appointment> appointments = Arrays.asList(testAppointment);
        when(appointmentService.getAppointmentsByPatient(patientId)).thenReturn(appointments);

        // Act
        List<Appointment> result = appointmentController.getAppointmentsByPatient(patientId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(appointmentService, times(1)).getAppointmentsByPatient(patientId);
    }

    @Test
    void testUpdateAppointment_Success() {
        // Arrange
        String appointmentId = "APT01";
        UpdateAppointmentRequest updateRequest = new UpdateAppointmentRequest();
        updateRequest.setStatus(AppointmentStatus.SCHEDULED);

        when(appointmentService.updateAppointment(appointmentId, updateRequest))
                .thenReturn(testAppointment);

        // Act
        ResponseEntity<?> response = appointmentController.updateAppointment(appointmentId, updateRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(appointmentService, times(1)).updateAppointment(appointmentId, updateRequest);
    }

    @Test
    void testUpdateAppointment_NotFound() {
        // Arrange
        String appointmentId = "APT99";
        UpdateAppointmentRequest updateRequest = new UpdateAppointmentRequest();
        when(appointmentService.updateAppointment(appointmentId, updateRequest))
                .thenReturn(null);

        // Act
        ResponseEntity<?> response = appointmentController.updateAppointment(appointmentId, updateRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testDeleteAppointment_Success() {
        // Arrange
        String appointmentId = "APT01";
        when(appointmentService.deleteAppointment(appointmentId)).thenReturn(true);

        // Act
        ResponseEntity<Void> response = appointmentController.deleteAppointment(appointmentId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(appointmentService, times(1)).deleteAppointment(appointmentId);
    }

    @Test
    void testDeleteAppointment_NotFound() {
        // Arrange
        String appointmentId = "APT99";
        when(appointmentService.deleteAppointment(appointmentId)).thenReturn(false);

        // Act
        ResponseEntity<Void> response = appointmentController.deleteAppointment(appointmentId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testGetAppointmentWithPatientAndRecord_Success() {
        // Arrange
        String appointmentId = "APT01";
        AppointmentDetailsDTO details = new AppointmentDetailsDTO(testAppointment);
        when(appointmentService.getAppointmentWithPatientAndRecord(appointmentId)).thenReturn(details);

        // Act
        ResponseEntity<AppointmentDetailsDTO> response = 
                appointmentController.getAppointmentWithPatientAndRecord(appointmentId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(appointmentService, times(1)).getAppointmentWithPatientAndRecord(appointmentId);
    }

    @Test
    void testGetAppointmentsByPhysicianWithPatients_Success() {
        // Arrange
        String physicianId = "PHY01";
        List<AppointmentDetailsDTO> appointments = Arrays.asList(new AppointmentDetailsDTO(testAppointment));
        when(appointmentService.getAppointmentsByPhysicianWithPatients(physicianId)).thenReturn(appointments);

        // Act
        ResponseEntity<List<AppointmentDetailsDTO>> response = 
                appointmentController.getAppointmentsByPhysicianWithPatients(physicianId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
    }
}

