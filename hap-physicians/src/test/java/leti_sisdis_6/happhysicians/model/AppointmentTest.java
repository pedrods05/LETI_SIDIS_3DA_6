package leti_sisdis_6.happhysicians.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AppointmentTest {

    private Appointment appointment;
    private Physician testPhysician;
    private Department testDepartment;
    private Specialty testSpecialty;

    @BeforeEach
    void setUp() {
        testDepartment = Department.builder()
                .departmentId("DEPT01")
                .code("CARD")
                .name("Cardiology")
                .build();

        testSpecialty = Specialty.builder()
                .specialtyId("SPEC01")
                .name("Cardiologist")
                .build();

        testPhysician = Physician.builder()
                .physicianId("PHY01")
                .fullName("Dr. John Doe")
                .licenseNumber("LIC123")
                .username("john.doe@hospital.com")
                .password("password")
                .specialty(testSpecialty)
                .department(testDepartment)
                .workingHourStart(java.time.LocalTime.of(9, 0))
                .workingHourEnd(java.time.LocalTime.of(17, 0))
                .build();

        appointment = new Appointment();
    }

    @Test
    void testNoArgsConstructor() {
        assertNotNull(appointment);
        assertNull(appointment.getAppointmentId());
        assertNull(appointment.getPatientId());
        assertNull(appointment.getPhysician());
        assertNull(appointment.getDateTime());
        assertNull(appointment.getConsultationType());
        assertNull(appointment.getStatus());
        assertFalse(appointment.isWasRescheduled());
    }

    @Test
    void testAllArgsConstructor() {
        LocalDateTime dateTime = LocalDateTime.now().plusDays(1);
        Appointment apt = new Appointment(
                "APT01",
                "PAT01",
                "John Patient",
                "patient@example.com",
                "123456789",
                testPhysician,
                dateTime,
                ConsultationType.FIRST_TIME,
                AppointmentStatus.SCHEDULED,
                false
        );

        assertEquals("APT01", apt.getAppointmentId());
        assertEquals("PAT01", apt.getPatientId());
        assertEquals("John Patient", apt.getPatientName());
        assertEquals("patient@example.com", apt.getPatientEmail());
        assertEquals("123456789", apt.getPatientPhone());
        assertEquals(testPhysician, apt.getPhysician());
        assertEquals(dateTime, apt.getDateTime());
        assertEquals(ConsultationType.FIRST_TIME, apt.getConsultationType());
        assertEquals(AppointmentStatus.SCHEDULED, apt.getStatus());
        assertFalse(apt.isWasRescheduled());
    }

    @Test
    void testBuilder() {
        LocalDateTime dateTime = LocalDateTime.now().plusDays(2);
        Appointment apt = Appointment.builder()
                .appointmentId("APT02")
                .patientId("PAT02")
                .patientName("Jane Patient")
                .patientEmail("jane@example.com")
                .patientPhone("987654321")
                .physician(testPhysician)
                .dateTime(dateTime)
                .consultationType(ConsultationType.FOLLOW_UP)
                .status(AppointmentStatus.SCHEDULED)
                .wasRescheduled(true)
                .build();

        assertEquals("APT02", apt.getAppointmentId());
        assertEquals("PAT02", apt.getPatientId());
        assertEquals("Jane Patient", apt.getPatientName());
        assertEquals("jane@example.com", apt.getPatientEmail());
        assertEquals("987654321", apt.getPatientPhone());
        assertEquals(testPhysician, apt.getPhysician());
        assertEquals(dateTime, apt.getDateTime());
        assertEquals(ConsultationType.FOLLOW_UP, apt.getConsultationType());
        assertEquals(AppointmentStatus.SCHEDULED, apt.getStatus());
        assertTrue(apt.isWasRescheduled());
    }

    @Test
    void testSettersAndGetters() {
        LocalDateTime dateTime = LocalDateTime.now().plusDays(3);

        appointment.setAppointmentId("APT03");
        appointment.setPatientId("PAT03");
        appointment.setPatientName("Bob Patient");
        appointment.setPatientEmail("bob@example.com");
        appointment.setPatientPhone("555555555");
        appointment.setPhysician(testPhysician);
        appointment.setDateTime(dateTime);
        appointment.setConsultationType(ConsultationType.FIRST_TIME);
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setWasRescheduled(false);

        assertEquals("APT03", appointment.getAppointmentId());
        assertEquals("PAT03", appointment.getPatientId());
        assertEquals("Bob Patient", appointment.getPatientName());
        assertEquals("bob@example.com", appointment.getPatientEmail());
        assertEquals("555555555", appointment.getPatientPhone());
        assertEquals(testPhysician, appointment.getPhysician());
        assertEquals(dateTime, appointment.getDateTime());
        assertEquals(ConsultationType.FIRST_TIME, appointment.getConsultationType());
        assertEquals(AppointmentStatus.COMPLETED, appointment.getStatus());
        assertFalse(appointment.isWasRescheduled());
    }

    @Test
    void testWasRescheduled() {
        appointment.setWasRescheduled(true);
        assertTrue(appointment.isWasRescheduled());

        appointment.setWasRescheduled(false);
        assertFalse(appointment.isWasRescheduled());
    }

    @Test
    void testBuilder_WithAllStatusTypes() {
        LocalDateTime dateTime = LocalDateTime.now();

        Appointment scheduled = Appointment.builder()
                .appointmentId("APT_SCHEDULED")
                .patientId("PAT01")
                .physician(testPhysician)
                .dateTime(dateTime)
                .consultationType(ConsultationType.FIRST_TIME)
                .status(AppointmentStatus.SCHEDULED)
                .wasRescheduled(false)
                .build();
        assertEquals(AppointmentStatus.SCHEDULED, scheduled.getStatus());

        Appointment canceled = Appointment.builder()
                .appointmentId("APT_CANCELED")
                .patientId("PAT01")
                .physician(testPhysician)
                .dateTime(dateTime)
                .consultationType(ConsultationType.FIRST_TIME)
                .status(AppointmentStatus.CANCELED)
                .wasRescheduled(false)
                .build();
        assertEquals(AppointmentStatus.CANCELED, canceled.getStatus());

        Appointment completed = Appointment.builder()
                .appointmentId("APT_COMPLETED")
                .patientId("PAT01")
                .physician(testPhysician)
                .dateTime(dateTime)
                .consultationType(ConsultationType.FIRST_TIME)
                .status(AppointmentStatus.COMPLETED)
                .wasRescheduled(false)
                .build();
        assertEquals(AppointmentStatus.COMPLETED, completed.getStatus());
    }
}

