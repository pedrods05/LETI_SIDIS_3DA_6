package leti_sisdis_6.happhysicians.repository;

import leti_sisdis_6.happhysicians.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class LocalDataRepositoryTest {

    private LocalDataRepository repository;

    private Department testDepartment;
    private Specialty testSpecialty;
    private Physician testPhysician;
    private Appointment testAppointment;

    @BeforeEach
    void setUp() {
        repository = new LocalDataRepository();

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
                .password("encodedPassword")
                .specialty(testSpecialty)
                .department(testDepartment)
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

    // ===== PHYSICIAN TESTS =====

    @Test
    void testSavePhysician_Success() {
        // Act
        Physician saved = repository.savePhysician(testPhysician);

        // Assert
        assertNotNull(saved);
        assertEquals("PHY01", saved.getPhysicianId());
    }

    @Test
    void testSavePhysician_NullPhysician() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            repository.savePhysician(null);
        });
    }

    @Test
    void testFindPhysicianById_Success() {
        // Arrange
        repository.savePhysician(testPhysician);

        // Act
        Optional<Physician> found = repository.findPhysicianById("PHY01");

        // Assert
        assertTrue(found.isPresent());
        assertEquals("PHY01", found.get().getPhysicianId());
    }

    @Test
    void testFindPhysicianById_NotFound() {
        // Act
        Optional<Physician> found = repository.findPhysicianById("PHY99");

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    void testFindAllPhysicians() {
        // Arrange
        repository.savePhysician(testPhysician);

        // Act
        List<Physician> physicians = repository.findAllPhysicians();

        // Assert
        assertEquals(1, physicians.size());
    }

    @Test
    void testExistsPhysicianByUsername() {
        // Arrange
        repository.savePhysician(testPhysician);

        // Act
        boolean exists = repository.existsPhysicianByUsername("john.doe@hospital.com");

        // Assert
        assertTrue(exists);
    }

    @Test
    void testExistsPhysicianByLicenseNumber() {
        // Arrange
        repository.savePhysician(testPhysician);

        // Act
        boolean exists = repository.existsPhysicianByLicenseNumber("LIC123");

        // Assert
        assertTrue(exists);
    }

    @Test
    void testFindPhysiciansBySpecialtyId() {
        // Arrange
        repository.savePhysician(testPhysician);

        // Act
        List<Physician> physicians = repository.findPhysiciansBySpecialtyId("SPEC01");

        // Assert
        assertEquals(1, physicians.size());
    }

    @Test
    void testDeletePhysicianById() {
        // Arrange
        repository.savePhysician(testPhysician);

        // Act
        repository.deletePhysicianById("PHY01");

        // Assert
        assertFalse(repository.existsPhysicianById("PHY01"));
    }

    // ===== APPOINTMENT TESTS =====

    @Test
    void testSaveAppointment_Success() {
        // Arrange
        repository.savePhysician(testPhysician);

        // Act
        Appointment saved = repository.saveAppointment(testAppointment);

        // Assert
        assertNotNull(saved);
        assertEquals("APT01", saved.getAppointmentId());
    }

    @Test
    void testSaveAppointment_NullAppointment() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            repository.saveAppointment(null);
        });
    }

    @Test
    void testFindAppointmentById_Success() {
        // Arrange
        repository.savePhysician(testPhysician);
        repository.saveAppointment(testAppointment);

        // Act
        Optional<Appointment> found = repository.findAppointmentById("APT01");

        // Assert
        assertTrue(found.isPresent());
        assertEquals("APT01", found.get().getAppointmentId());
    }

    @Test
    void testFindAppointmentsByPatientId() {
        // Arrange
        repository.savePhysician(testPhysician);
        repository.saveAppointment(testAppointment);

        // Act
        List<Appointment> appointments = repository.findAppointmentsByPatientId("PAT01");

        // Assert
        assertEquals(1, appointments.size());
    }

    @Test
    void testFindAppointmentsByPhysicianId() {
        // Arrange
        repository.savePhysician(testPhysician);
        repository.saveAppointment(testAppointment);

        // Act
        List<Appointment> appointments = repository.findAppointmentsByPhysicianId("PHY01");

        // Assert
        assertEquals(1, appointments.size());
    }

    @Test
    void testFindAppointmentsByDateTimeBetween() {
        // Arrange
        repository.savePhysician(testPhysician);
        repository.saveAppointment(testAppointment);
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = LocalDateTime.now().plusDays(2);

        // Act
        List<Appointment> appointments = repository.findAppointmentsByDateTimeBetween(start, end);

        // Assert
        assertEquals(1, appointments.size());
    }

    @Test
    void testDeleteAppointmentById() {
        // Arrange
        repository.savePhysician(testPhysician);
        repository.saveAppointment(testAppointment);

        // Act
        repository.deleteAppointmentById("APT01");

        // Assert
        assertFalse(repository.existsAppointmentById("APT01"));
    }

    // ===== DEPARTMENT TESTS =====

    @Test
    void testSaveDepartment_Success() {
        // Act
        Department saved = repository.saveDepartment(testDepartment);

        // Assert
        assertNotNull(saved);
        assertEquals("DEPT01", saved.getDepartmentId());
    }

    @Test
    void testFindDepartmentById_Success() {
        // Arrange
        repository.saveDepartment(testDepartment);

        // Act
        Optional<Department> found = repository.findDepartmentById("DEPT01");

        // Assert
        assertTrue(found.isPresent());
        assertEquals("DEPT01", found.get().getDepartmentId());
    }

    @Test
    void testExistsDepartmentByCode() {
        // Arrange
        repository.saveDepartment(testDepartment);

        // Act
        boolean exists = repository.existsDepartmentByCode("CARD");

        // Assert
        assertTrue(exists);
    }

    // ===== SPECIALTY TESTS =====

    @Test
    void testSaveSpecialty_Success() {
        // Act
        Specialty saved = repository.saveSpecialty(testSpecialty);

        // Assert
        assertNotNull(saved);
        assertEquals("SPEC01", saved.getSpecialtyId());
    }

    @Test
    void testFindSpecialtyById_Success() {
        // Arrange
        repository.saveSpecialty(testSpecialty);

        // Act
        Optional<Specialty> found = repository.findSpecialtyById("SPEC01");

        // Assert
        assertTrue(found.isPresent());
        assertEquals("SPEC01", found.get().getSpecialtyId());
    }

    @Test
    void testExistsSpecialtyByName() {
        // Arrange
        repository.saveSpecialty(testSpecialty);

        // Act
        boolean exists = repository.existsSpecialtyByName("Cardiologist");

        // Assert
        assertTrue(exists);
    }

    // ===== UTILITY TESTS =====

    @Test
    void testClearAll() {
        // Arrange
        repository.savePhysician(testPhysician);
        repository.saveAppointment(testAppointment);
        repository.saveDepartment(testDepartment);
        repository.saveSpecialty(testSpecialty);

        // Act
        repository.clearAll();

        // Assert
        Map<String, Integer> counts = repository.getDataCounts();
        assertEquals(0, counts.get("physicians"));
        assertEquals(0, counts.get("appointments"));
        assertEquals(0, counts.get("departments"));
        assertEquals(0, counts.get("specialties"));
    }

    @Test
    void testGetDataCounts() {
        // Arrange
        repository.savePhysician(testPhysician);
        repository.saveAppointment(testAppointment);
        repository.saveDepartment(testDepartment);
        repository.saveSpecialty(testSpecialty);

        // Act
        Map<String, Integer> counts = repository.getDataCounts();

        // Assert
        assertEquals(1, counts.get("physicians"));
        assertEquals(1, counts.get("appointments"));
        assertEquals(1, counts.get("departments"));
        assertEquals(1, counts.get("specialties"));
    }

    @Test
    void testFindTop5PhysiciansByAppointmentCount() {
        // Arrange
        repository.savePhysician(testPhysician);
        repository.saveAppointment(testAppointment);
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now().plusDays(2);

        // Act
        List<Object[]> topPhysicians = repository.findTop5PhysiciansByAppointmentCount(from, to);

        // Assert
        assertNotNull(topPhysicians);
        assertEquals(1, topPhysicians.size());
        assertEquals("PHY01", topPhysicians.get(0)[0]);
    }
}

