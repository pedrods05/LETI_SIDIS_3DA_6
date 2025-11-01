package leti_sisdis_6.happhysicians.dto.output;

import leti_sisdis_6.happhysicians.model.Appointment;
import leti_sisdis_6.happhysicians.model.AppointmentStatus;
import leti_sisdis_6.happhysicians.model.ConsultationType;
import leti_sisdis_6.happhysicians.model.Department;
import leti_sisdis_6.happhysicians.model.Physician;
import leti_sisdis_6.happhysicians.model.Specialty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class AppointmentDetailsDTOTest {

    private Appointment testAppointment;
    private Physician testPhysician;

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
                .patientName("John Patient")
                .patientEmail("patient@example.com")
                .patientPhone("123456789")
                .physician(testPhysician)
                .dateTime(appointmentDateTime)
                .consultationType(ConsultationType.FIRST_TIME)
                .status(AppointmentStatus.SCHEDULED)
                .wasRescheduled(false)
                .build();
    }

    @Test
    void testConstructor_WithAppointment_Success() {
        // Act
        AppointmentDetailsDTO dto = new AppointmentDetailsDTO(testAppointment);

        // Assert
        assertNotNull(dto);
        assertEquals("APT01", dto.getAppointmentId());
        assertEquals("PAT01", dto.getPatientId());
        assertEquals("John Patient", dto.getPatientName());
        assertEquals("patient@example.com", dto.getPatientEmail());
        assertEquals("123456789", dto.getPatientPhone());
        assertEquals("PHY01", dto.getPhysicianId());
        assertEquals("Dr. John Doe", dto.getPhysicianName());
        assertEquals(testAppointment.getDateTime(), dto.getDateTime());
        assertEquals(ConsultationType.FIRST_TIME, dto.getConsultationType());
        assertEquals(AppointmentStatus.SCHEDULED, dto.getStatus());
        assertEquals(false, dto.isWasRescheduled());
    }

    @Test
    void testConstructor_WithNullPatientData() {
        // Arrange
        Appointment appointmentWithoutPatientData = Appointment.builder()
                .appointmentId("APT02")
                .patientId("PAT02")
                .patientName(null)
                .patientEmail(null)
                .patientPhone(null)
                .physician(testPhysician)
                .dateTime(LocalDateTime.now())
                .consultationType(ConsultationType.FIRST_TIME)
                .status(AppointmentStatus.SCHEDULED)
                .wasRescheduled(false)
                .build();

        // Act
        AppointmentDetailsDTO dto = new AppointmentDetailsDTO(appointmentWithoutPatientData);

        // Assert
        assertNotNull(dto);
        assertEquals("APT02", dto.getAppointmentId());
        assertNull(dto.getPatientName());
        assertNull(dto.getPatientEmail());
        assertNull(dto.getPatientPhone());
        assertEquals("PHY01", dto.getPhysicianId());
    }

    @Test
    void testBuilder_WorksCorrectly() {
        // Act
        AppointmentDetailsDTO dto = AppointmentDetailsDTO.builder()
                .appointmentId("APT03")
                .patientId("PAT03")
                .patientName("Jane Patient")
                .physicianId("PHY02")
                .physicianName("Dr. Jane Smith")
                .dateTime(LocalDateTime.now())
                .consultationType(ConsultationType.FOLLOW_UP)
                .status(AppointmentStatus.SCHEDULED)
                .wasRescheduled(true)
                .build();

        // Assert
        assertNotNull(dto);
        assertEquals("APT03", dto.getAppointmentId());
        assertEquals("PAT03", dto.getPatientId());
        assertEquals("Jane Patient", dto.getPatientName());
        assertEquals("PHY02", dto.getPhysicianId());
        assertEquals("Dr. Jane Smith", dto.getPhysicianName());
        assertEquals(ConsultationType.FOLLOW_UP, dto.getConsultationType());
        assertEquals(AppointmentStatus.SCHEDULED, dto.getStatus());
        assertEquals(true, dto.isWasRescheduled());
    }

    @Test
    void testNoArgsConstructor_Works() {
        // Act
        AppointmentDetailsDTO dto = new AppointmentDetailsDTO();

        // Assert
        assertNotNull(dto);
        assertNull(dto.getAppointmentId());
        assertNull(dto.getPatientId());
    }
}

