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

}

