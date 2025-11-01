package leti_sisdis_6.happhysicians.repository;

import leti_sisdis_6.happhysicians.model.Department;
import leti_sisdis_6.happhysicians.model.Physician;
import leti_sisdis_6.happhysicians.model.Specialty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.docker.compose.enabled=false"
})
class PhysicianRepositoryTest {

    @Autowired
    private PhysicianRepository physicianRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private SpecialtyRepository specialtyRepository;

    private Department testDepartment;
    private Specialty testSpecialty;
    private Physician testPhysician;

    @BeforeEach
    void setUp() {
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
    }

    @Test
    void testSavePhysician() {
        Physician saved = physicianRepository.save(testPhysician);

        assertNotNull(saved);
        assertEquals("PHY01", saved.getPhysicianId());
        assertEquals("Dr. John Doe", saved.getFullName());
        assertEquals("LIC123", saved.getLicenseNumber());
        assertEquals("john.doe@hospital.com", saved.getUsername());
    }

    @Test
    void testFindById() {
        physicianRepository.save(testPhysician);

        Optional<Physician> found = physicianRepository.findById("PHY01");

        assertTrue(found.isPresent());
        assertEquals("PHY01", found.get().getPhysicianId());
        assertEquals("Dr. John Doe", found.get().getFullName());
    }

    @Test
    void testFindById_NotFound() {
        Optional<Physician> found = physicianRepository.findById("NONEXISTENT");

        assertFalse(found.isPresent());
    }

    @Test
    void testExistsByUsername_True() {
        physicianRepository.save(testPhysician);

        boolean exists = physicianRepository.existsByUsername("john.doe@hospital.com");

        assertTrue(exists);
    }

    @Test
    void testExistsByUsername_False() {
        physicianRepository.save(testPhysician);

        boolean exists = physicianRepository.existsByUsername("other@hospital.com");

        assertFalse(exists);
    }

    @Test
    void testExistsByLicenseNumber_True() {
        physicianRepository.save(testPhysician);

        boolean exists = physicianRepository.existsByLicenseNumber("LIC123");

        assertTrue(exists);
    }

    @Test
    void testExistsByLicenseNumber_False() {
        physicianRepository.save(testPhysician);

        boolean exists = physicianRepository.existsByLicenseNumber("LIC999");

        assertFalse(exists);
    }

    @Test
    void testFindByUsername() {
        physicianRepository.save(testPhysician);

        Optional<Physician> found = physicianRepository.findByUsername("john.doe@hospital.com");

        assertTrue(found.isPresent());
        assertEquals("PHY01", found.get().getPhysicianId());
        assertEquals("john.doe@hospital.com", found.get().getUsername());
    }

    @Test
    void testFindByUsername_NotFound() {
        Optional<Physician> found = physicianRepository.findByUsername("nonexistent@hospital.com");

        assertFalse(found.isPresent());
    }

    @Test
    void testFindBySpecialtySpecialtyId() {
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

        Specialty spec2 = Specialty.builder()
                .specialtyId("SPEC02")
                .name("Dermatologist")
                .build();
        specialtyRepository.save(spec2);

        Physician phy3 = Physician.builder()
                .physicianId("PHY03")
                .fullName("Dr. Bob Brown")
                .licenseNumber("LIC789")
                .username("bob@hospital.com")
                .password("password")
                .specialty(spec2)
                .department(testDepartment)
                .workingHourStart(LocalTime.of(9, 0))
                .workingHourEnd(LocalTime.of(17, 0))
                .build();
        physicianRepository.save(phy3);

        List<Physician> cardiologists = physicianRepository.findBySpecialtySpecialtyId("SPEC01");

        assertEquals(2, cardiologists.size());
        assertTrue(cardiologists.stream().allMatch(p -> p.getSpecialty().getSpecialtyId().equals("SPEC01")));
    }

    @Test
    void testFindByDepartmentDepartmentId() {
        physicianRepository.save(testPhysician);

        Department dept2 = Department.builder()
                .departmentId("DEPT02")
                .code("DERM")
                .name("Dermatology")
                .build();
        departmentRepository.save(dept2);

        Physician phy2 = Physician.builder()
                .physicianId("PHY02")
                .fullName("Dr. Jane Smith")
                .licenseNumber("LIC456")
                .username("jane@hospital.com")
                .password("password")
                .specialty(testSpecialty)
                .department(dept2)
                .workingHourStart(LocalTime.of(9, 0))
                .workingHourEnd(LocalTime.of(17, 0))
                .build();
        physicianRepository.save(phy2);

        List<Physician> cardiologyDept = physicianRepository.findByDepartmentDepartmentId("DEPT01");

        assertEquals(1, cardiologyDept.size());
        assertEquals("PHY01", cardiologyDept.get(0).getPhysicianId());
    }

    @Test
    void testSearchByNameOrSpecialty_ByName() {
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

        List<Physician> results = physicianRepository.searchByNameOrSpecialty("John", null, null, null);

        assertEquals(1, results.size());
        assertEquals("PHY01", results.get(0).getPhysicianId());
    }

    @Test
    void testSearchByNameOrSpecialty_BySpecialty() {
        physicianRepository.save(testPhysician);

        Specialty spec2 = Specialty.builder()
                .specialtyId("SPEC02")
                .name("Dermatologist")
                .build();
        specialtyRepository.save(spec2);

        Physician phy2 = Physician.builder()
                .physicianId("PHY02")
                .fullName("Dr. Jane Smith")
                .licenseNumber("LIC456")
                .username("jane@hospital.com")
                .password("password")
                .specialty(spec2)
                .department(testDepartment)
                .workingHourStart(LocalTime.of(9, 0))
                .workingHourEnd(LocalTime.of(17, 0))
                .build();
        physicianRepository.save(phy2);

        List<Physician> results = physicianRepository.searchByNameOrSpecialty(null, null, null, "Cardiologist");

        assertEquals(1, results.size());
        assertEquals("PHY01", results.get(0).getPhysicianId());
        assertEquals("Cardiologist", results.get(0).getSpecialty().getName());
    }

    @Test
    void testSearchByNameOrSpecialty_ByNameAndSpecialty() {
        physicianRepository.save(testPhysician);

        Specialty spec2 = Specialty.builder()
                .specialtyId("SPEC02")
                .name("Dermatologist")
                .build();
        specialtyRepository.save(spec2);

        Physician phy2 = Physician.builder()
                .physicianId("PHY02")
                .fullName("Dr. Jane Smith")
                .licenseNumber("LIC456")
                .username("jane@hospital.com")
                .password("password")
                .specialty(spec2)
                .department(testDepartment)
                .workingHourStart(LocalTime.of(9, 0))
                .workingHourEnd(LocalTime.of(17, 0))
                .build();
        physicianRepository.save(phy2);

        List<Physician> results = physicianRepository.searchByNameOrSpecialty("John", null, null, "Cardiologist");

        assertEquals(1, results.size());
        assertEquals("PHY01", results.get(0).getPhysicianId());
    }

    @Test
    void testSearchByNameOrSpecialty_NoResults() {
        physicianRepository.save(testPhysician);

        List<Physician> results = physicianRepository.searchByNameOrSpecialty("NonExistent", null, null, null);

        assertTrue(results.isEmpty());
    }

    @Test
    void testDelete() {
        Physician saved = physicianRepository.save(testPhysician);

        physicianRepository.delete(saved);

        Optional<Physician> found = physicianRepository.findById("PHY01");
        assertFalse(found.isPresent());
    }

    @Test
    void testFindAll() {
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

        physicianRepository.save(testPhysician);
        physicianRepository.save(phy2);

        List<Physician> all = physicianRepository.findAll();

        assertEquals(2, all.size());
    }
}

