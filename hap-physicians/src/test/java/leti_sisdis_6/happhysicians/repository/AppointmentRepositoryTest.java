package leti_sisdis_6.happhysicians.repository;

import leti_sisdis_6.happhysicians.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.docker.compose.enabled=false"
})
class AppointmentRepositoryTest {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PhysicianRepository physicianRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private SpecialtyRepository specialtyRepository;

    private Department testDepartment;
    private Specialty testSpecialty;
    private Physician testPhysician;
    private Appointment testAppointment;

    @BeforeEach
    void setUp() {
        appointmentRepository.deleteAll();
        physicianRepository.deleteAll();
        departmentRepository.deleteAll();
        specialtyRepository.deleteAll();

        testDepartment = Department.builder()
                .departmentId("DEPT01")
                .code("CARD")
                .name("Cardiology")
                .build();
        departmentRepository.save(testDepartment);

        testSpecialty = Specialty.builder()
                .specialtyId("SPEC01")
                .name("Cardiologist")
                .build();
        specialtyRepository.save(testSpecialty);

        testPhysician = Physician.builder()
                .physicianId("PHY01")
                .fullName("Dr. John Doe")
                .licenseNumber("LIC123")
                .username("john.doe@hospital.com")
                .password("encodedPassword")
                .specialty(testSpecialty)
                .department(testDepartment)
                .workingHourStart(LocalTime.of(9, 0))
                .workingHourEnd(LocalTime.of(17, 0))
                .build();
        physicianRepository.save(testPhysician);

        testAppointment = Appointment.builder()
                .appointmentId("APT01")
                .patientId("PAT01")
                .patientName("John Patient")
                .patientEmail("patient@example.com")
                .patientPhone("123456789")
                .physician(testPhysician)
                .dateTime(LocalDateTime.now().plusDays(1))
                .consultationType(ConsultationType.FIRST_TIME)
                .status(AppointmentStatus.SCHEDULED)
                .wasRescheduled(false)
                .build();
    }

    @Test
    void testSaveAppointment() {
        Appointment saved = appointmentRepository.save(testAppointment);

        assertNotNull(saved);
        assertEquals("APT01", saved.getAppointmentId());
        assertEquals("PAT01", saved.getPatientId());
        assertEquals("John Patient", saved.getPatientName());
        assertNotNull(saved.getPhysician());
        assertEquals("PHY01", saved.getPhysician().getPhysicianId());
        assertEquals("Dr. John Doe", saved.getPhysician().getFullName());
    }

    @Test
    void testFindById() {
        appointmentRepository.save(testAppointment);

        Optional<Appointment> found = appointmentRepository.findById("APT01");

        assertTrue(found.isPresent());
        assertEquals("APT01", found.get().getAppointmentId());
        assertEquals("PAT01", found.get().getPatientId());
    }

    @Test
    void testFindById_NotFound() {
        Optional<Appointment> found = appointmentRepository.findById("NONEXISTENT");

        assertFalse(found.isPresent());
    }

    @Test
    void testExistsByPhysicianPhysicianIdAndDateTime() {
        LocalDateTime dateTime = LocalDateTime.now().plusDays(1).withMinute(0).withSecond(0).withNano(0);
        testAppointment.setDateTime(dateTime);
        appointmentRepository.save(testAppointment);
        
        // Use the same dateTime to avoid precision issues
        boolean exists = appointmentRepository.existsByPhysicianPhysicianIdAndDateTime("PHY01", dateTime);

        assertTrue(exists);
    }

    @Test
    void testExistsByPhysicianPhysicianIdAndDateTime_False() {
        LocalDateTime dateTime = LocalDateTime.now().plusDays(1);
        testAppointment.setDateTime(dateTime);
        appointmentRepository.save(testAppointment);

        LocalDateTime differentDateTime = LocalDateTime.now().plusDays(2);
        boolean exists = appointmentRepository.existsByPhysicianPhysicianIdAndDateTime("PHY01", differentDateTime);

        assertFalse(exists);
    }

    @Test
    void testFindByPatientId() {
        appointmentRepository.save(testAppointment);

        Appointment apt2 = Appointment.builder()
                .appointmentId("APT02")
                .patientId("PAT01")
                .patientName("John Patient")
                .patientEmail("patient@example.com")
                .patientPhone("123456789")
                .physician(testPhysician)
                .dateTime(LocalDateTime.now().plusDays(2))
                .consultationType(ConsultationType.FOLLOW_UP)
                .status(AppointmentStatus.SCHEDULED)
                .wasRescheduled(false)
                .build();
        appointmentRepository.save(apt2);

        List<Appointment> appointments = appointmentRepository.findByPatientId("PAT01");

        assertEquals(2, appointments.size());
        assertTrue(appointments.stream().allMatch(a -> a.getPatientId().equals("PAT01")));
    }

    @Test
    void testFindByPatientIdOrderByDateTimeDesc() {
        LocalDateTime dateTime1 = LocalDateTime.now().plusDays(1);
        LocalDateTime dateTime2 = LocalDateTime.now().plusDays(2);
        LocalDateTime dateTime3 = LocalDateTime.now().plusDays(3);

        testAppointment.setDateTime(dateTime2);
        appointmentRepository.save(testAppointment);

        Appointment apt2 = Appointment.builder()
                .appointmentId("APT02")
                .patientId("PAT01")
                .patientName("John Patient")
                .physician(testPhysician)
                .dateTime(dateTime1)
                .consultationType(ConsultationType.FIRST_TIME)
                .status(AppointmentStatus.SCHEDULED)
                .wasRescheduled(false)
                .build();
        appointmentRepository.save(apt2);

        Appointment apt3 = Appointment.builder()
                .appointmentId("APT03")
                .patientId("PAT01")
                .patientName("John Patient")
                .physician(testPhysician)
                .dateTime(dateTime3)
                .consultationType(ConsultationType.FIRST_TIME)
                .status(AppointmentStatus.SCHEDULED)
                .wasRescheduled(false)
                .build();
        appointmentRepository.save(apt3);

        List<Appointment> appointments = appointmentRepository.findByPatientIdOrderByDateTimeDesc("PAT01");

        assertEquals(3, appointments.size());
        assertEquals("APT03", appointments.get(0).getAppointmentId()); // Most recent first
        assertEquals("APT01", appointments.get(1).getAppointmentId());
        assertEquals("APT02", appointments.get(2).getAppointmentId()); // Oldest last
    }

    @Test
    void testFindByDateTimeAfterOrderByDateTimeAsc() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime past = now.minusDays(1);
        LocalDateTime future1 = now.plusDays(1);
        LocalDateTime future2 = now.plusDays(2);

        Appointment pastApt = Appointment.builder()
                .appointmentId("APT_PAST")
                .patientId("PAT01")
                .patientName("John Patient")
                .physician(testPhysician)
                .dateTime(past)
                .consultationType(ConsultationType.FIRST_TIME)
                .status(AppointmentStatus.COMPLETED)
                .wasRescheduled(false)
                .build();
        appointmentRepository.save(pastApt);

        testAppointment.setDateTime(future2);
        appointmentRepository.save(testAppointment);

        Appointment apt2 = Appointment.builder()
                .appointmentId("APT_FUTURE")
                .patientId("PAT02")
                .patientName("Jane Patient")
                .physician(testPhysician)
                .dateTime(future1)
                .consultationType(ConsultationType.FIRST_TIME)
                .status(AppointmentStatus.SCHEDULED)
                .wasRescheduled(false)
                .build();
        appointmentRepository.save(apt2);

        List<Appointment> futureAppointments = appointmentRepository.findByDateTimeAfterOrderByDateTimeAsc(now);

        assertEquals(2, futureAppointments.size());
        assertEquals("APT_FUTURE", futureAppointments.get(0).getAppointmentId()); // Earlier first
        assertEquals("APT01", futureAppointments.get(1).getAppointmentId()); // Later second
    }

    @Test
    void testFindByPhysicianPhysicianIdAndDateTimeBetween() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0);
        LocalDateTime end = LocalDateTime.now().plusDays(3).withHour(17).withMinute(0);

        LocalDateTime within1 = LocalDateTime.now().plusDays(2);
        LocalDateTime within2 = LocalDateTime.now().plusDays(2).withHour(14);
        LocalDateTime before = LocalDateTime.now();
        LocalDateTime after = LocalDateTime.now().plusDays(4);

        testAppointment.setDateTime(within1);
        appointmentRepository.save(testAppointment);

        Appointment apt2 = Appointment.builder()
                .appointmentId("APT02")
                .patientId("PAT02")
                .patientName("Jane Patient")
                .physician(testPhysician)
                .dateTime(within2)
                .consultationType(ConsultationType.FIRST_TIME)
                .status(AppointmentStatus.SCHEDULED)
                .wasRescheduled(false)
                .build();
        appointmentRepository.save(apt2);

        Appointment apt3 = Appointment.builder()
                .appointmentId("APT03")
                .patientId("PAT03")
                .patientName("Bob Patient")
                .physician(testPhysician)
                .dateTime(before)
                .consultationType(ConsultationType.FIRST_TIME)
                .status(AppointmentStatus.SCHEDULED)
                .wasRescheduled(false)
                .build();
        appointmentRepository.save(apt3);

        Appointment apt4 = Appointment.builder()
                .appointmentId("APT04")
                .patientId("PAT04")
                .patientName("Alice Patient")
                .physician(testPhysician)
                .dateTime(after)
                .consultationType(ConsultationType.FIRST_TIME)
                .status(AppointmentStatus.SCHEDULED)
                .wasRescheduled(false)
                .build();
        appointmentRepository.save(apt4);

        List<Appointment> appointments = appointmentRepository.findByPhysicianPhysicianIdAndDateTimeBetween("PHY01", start, end);

        assertEquals(2, appointments.size());
        assertTrue(appointments.stream().allMatch(a -> 
            a.getDateTime().isAfter(start.minusSeconds(1)) && a.getDateTime().isBefore(end.plusSeconds(1))
        ));
    }

    @Test
    void testFindByDateTimeBetween() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(3);

        LocalDateTime within1 = LocalDateTime.now().plusDays(2);
        LocalDateTime within2 = LocalDateTime.now().plusDays(2).withHour(14);
        LocalDateTime before = LocalDateTime.now().minusDays(1);
        LocalDateTime after = LocalDateTime.now().plusDays(4);

        testAppointment.setDateTime(within1);
        appointmentRepository.save(testAppointment);

        Appointment apt2 = Appointment.builder()
                .appointmentId("APT02")
                .patientId("PAT02")
                .physician(testPhysician)
                .dateTime(within2)
                .consultationType(ConsultationType.FIRST_TIME)
                .status(AppointmentStatus.SCHEDULED)
                .wasRescheduled(false)
                .build();
        appointmentRepository.save(apt2);

        Appointment apt3 = Appointment.builder()
                .appointmentId("APT03")
                .patientId("PAT03")
                .physician(testPhysician)
                .dateTime(before)
                .consultationType(ConsultationType.FIRST_TIME)
                .status(AppointmentStatus.SCHEDULED)
                .wasRescheduled(false)
                .build();
        appointmentRepository.save(apt3);

        Appointment apt4 = Appointment.builder()
                .appointmentId("APT04")
                .patientId("PAT04")
                .physician(testPhysician)
                .dateTime(after)
                .consultationType(ConsultationType.FIRST_TIME)
                .status(AppointmentStatus.SCHEDULED)
                .wasRescheduled(false)
                .build();
        appointmentRepository.save(apt4);

        List<Appointment> appointments = appointmentRepository.findByDateTimeBetween(start, end);

        assertEquals(2, appointments.size());
    }

    @Test
    void testFindByPatientIdAndStatus() {
        Appointment apt2 = Appointment.builder()
                .appointmentId("APT02")
                .patientId("PAT01")
                .patientName("John Patient")
                .physician(testPhysician)
                .dateTime(LocalDateTime.now().plusDays(2))
                .consultationType(ConsultationType.FIRST_TIME)
                .status(AppointmentStatus.CANCELED)
                .wasRescheduled(false)
                .build();

        testAppointment.setStatus(AppointmentStatus.SCHEDULED);
        appointmentRepository.save(testAppointment);
        appointmentRepository.save(apt2);

        List<Appointment> scheduled = appointmentRepository.findByPatientIdAndStatus("PAT01", AppointmentStatus.SCHEDULED);
        List<Appointment> canceled = appointmentRepository.findByPatientIdAndStatus("PAT01", AppointmentStatus.CANCELED);

        assertEquals(1, scheduled.size());
        assertEquals(AppointmentStatus.SCHEDULED, scheduled.get(0).getStatus());

        assertEquals(1, canceled.size());
        assertEquals(AppointmentStatus.CANCELED, canceled.get(0).getStatus());
    }

    @Test
    void testFindByPhysicianPhysicianId() {
        Physician phy2 = Physician.builder()
                .physicianId("PHY02")
                .fullName("Dr. Jane Smith")
                .licenseNumber("LIC456")
                .username("jane@hospital.com")
                .password("password")
                .specialty(testSpecialty)
                .department(testDepartment)
                .workingHourStart(LocalTime.of(9, 0))
                .workingHourEnd(LocalTime.of(17, 0))
                .build();
        physicianRepository.save(phy2);

        appointmentRepository.save(testAppointment);

        Appointment apt2 = Appointment.builder()
                .appointmentId("APT02")
                .patientId("PAT02")
                .patientName("Jane Patient")
                .physician(phy2)
                .dateTime(LocalDateTime.now().plusDays(2))
                .consultationType(ConsultationType.FIRST_TIME)
                .status(AppointmentStatus.SCHEDULED)
                .wasRescheduled(false)
                .build();
        appointmentRepository.save(apt2);

        List<Appointment> phy1Appointments = appointmentRepository.findByPhysicianPhysicianId("PHY01");
        List<Appointment> phy2Appointments = appointmentRepository.findByPhysicianPhysicianId("PHY02");

        assertEquals(1, phy1Appointments.size());
        assertEquals("PHY01", phy1Appointments.get(0).getPhysician().getPhysicianId());

        assertEquals(1, phy2Appointments.size());
        assertEquals("PHY02", phy2Appointments.get(0).getPhysician().getPhysicianId());
    }

    @Test
    void testFindTop5PhysiciansByAppointmentCount() {
        // Ensure testPhysician is saved
        physicianRepository.save(testPhysician);
        
        Physician phy2 = Physician.builder()
                .physicianId("PHY02")
                .fullName("Dr. Jane Smith")
                .licenseNumber("LIC456")
                .username("jane@hospital.com")
                .password("password")
                .specialty(testSpecialty)
                .department(testDepartment)
                .workingHourStart(LocalTime.of(9, 0))
                .workingHourEnd(LocalTime.of(17, 0))
                .build();
        physicianRepository.save(phy2);

        LocalDateTime now = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        LocalDateTime start = now.minusDays(30);
        LocalDateTime end = now.plusDays(10);

        // Create 5 appointments for PHY01
        for (int i = 1; i <= 5; i++) {
            Appointment apt = Appointment.builder()
                    .appointmentId("APT0" + i)
                    .patientId("PAT0" + i)
                    .patientName("Patient " + i)
                    .physician(testPhysician)
                    .dateTime(now.plusDays(i))
                    .consultationType(ConsultationType.FIRST_TIME)
                    .status(AppointmentStatus.SCHEDULED)
                    .wasRescheduled(false)
                    .build();
            appointmentRepository.save(apt);
        }

        // Create 3 appointments for PHY02
        for (int i = 6; i <= 8; i++) {
            Appointment apt = Appointment.builder()
                    .appointmentId("APT0" + i)
                    .patientId("PAT0" + i)
                    .patientName("Patient " + i)
                    .physician(phy2)
                    .dateTime(now.plusDays(i))
                    .consultationType(ConsultationType.FIRST_TIME)
                    .status(AppointmentStatus.SCHEDULED)
                    .wasRescheduled(false)
                    .build();
            appointmentRepository.save(apt);
        }

        List<Object[]> results = appointmentRepository.findTop5PhysiciansByAppointmentCount(start, end);

        assertFalse(results.isEmpty(), "Results should not be empty");
        assertEquals(2, results.size(), "Should have 2 physicians"); // Two physicians

        // PHY01 should be first (5 appointments)
        Object[] first = results.get(0);
        assertEquals("PHY01", first[0]);
        assertEquals(5L, first[3]); // appointmentCount

        // PHY02 should be second (3 appointments)
        Object[] second = results.get(1);
        assertEquals("PHY02", second[0]);
        assertEquals(3L, second[3]); // appointmentCount
    }

    @Test
    void testDelete() {
        Appointment saved = appointmentRepository.save(testAppointment);

        appointmentRepository.delete(saved);

        Optional<Appointment> found = appointmentRepository.findById("APT01");
        assertFalse(found.isPresent());
    }

    @Test
    void testFindAll() {
        Appointment apt2 = Appointment.builder()
                .appointmentId("APT02")
                .patientId("PAT02")
                .patientName("Jane Patient")
                .physician(testPhysician)
                .dateTime(LocalDateTime.now().plusDays(2))
                .consultationType(ConsultationType.FIRST_TIME)
                .status(AppointmentStatus.SCHEDULED)
                .wasRescheduled(false)
                .build();

        appointmentRepository.save(testAppointment);
        appointmentRepository.save(apt2);

        List<Appointment> all = appointmentRepository.findAll();

        assertEquals(2, all.size());
    }
}

