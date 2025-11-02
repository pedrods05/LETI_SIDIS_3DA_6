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

}

