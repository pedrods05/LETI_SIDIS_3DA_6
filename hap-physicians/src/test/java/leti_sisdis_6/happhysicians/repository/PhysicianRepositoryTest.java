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
    void testSearchByNameOrSpecialty() {
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

        // Test by name
        List<Physician> byName = physicianRepository.searchByNameOrSpecialty("John", null, null, null);
        assertEquals(1, byName.size());
        assertEquals("PHY01", byName.get(0).getPhysicianId());

        // Test by specialty
        List<Physician> bySpecialty = physicianRepository.searchByNameOrSpecialty(null, null, null, "Dermatologist");
        assertEquals(1, bySpecialty.size());
        assertEquals("PHY02", bySpecialty.get(0).getPhysicianId());

        // Test combined
        List<Physician> combined = physicianRepository.searchByNameOrSpecialty("John", null, null, "Cardiologist");
        assertEquals(1, combined.size());
        assertEquals("PHY01", combined.get(0).getPhysicianId());

        // Test no results
        List<Physician> noResults = physicianRepository.searchByNameOrSpecialty("NonExistent", null, null, null);
        assertTrue(noResults.isEmpty());
    }

}

