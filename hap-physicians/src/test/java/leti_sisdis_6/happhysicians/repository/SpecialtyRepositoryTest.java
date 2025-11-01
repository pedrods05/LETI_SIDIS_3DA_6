package leti_sisdis_6.happhysicians.repository;

import leti_sisdis_6.happhysicians.model.Specialty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.docker.compose.enabled=false"
})
class SpecialtyRepositoryTest {

    @Autowired
    private SpecialtyRepository specialtyRepository;

    private Specialty testSpecialty;

    @BeforeEach
    void setUp() {
        specialtyRepository.deleteAll();
        
        testSpecialty = Specialty.builder()
                .specialtyId("SPEC01")
                .name("Cardiologist")
                .build();
    }

    @Test
    void testSaveSpecialty() {
        Specialty saved = specialtyRepository.save(testSpecialty);
        
        assertNotNull(saved);
        assertEquals("SPEC01", saved.getSpecialtyId());
        assertEquals("Cardiologist", saved.getName());
    }

    @Test
    void testFindById() {
        specialtyRepository.save(testSpecialty);
        
        Optional<Specialty> found = specialtyRepository.findById("SPEC01");
        
        assertTrue(found.isPresent());
        assertEquals("SPEC01", found.get().getSpecialtyId());
        assertEquals("Cardiologist", found.get().getName());
    }

    @Test
    void testFindById_NotFound() {
        Optional<Specialty> found = specialtyRepository.findById("NONEXISTENT");
        
        assertFalse(found.isPresent());
    }

    @Test
    void testExistsByName_True() {
        specialtyRepository.save(testSpecialty);
        
        boolean exists = specialtyRepository.existsByName("Cardiologist");
        
        assertTrue(exists);
    }

    @Test
    void testExistsByName_False() {
        specialtyRepository.save(testSpecialty);
        
        boolean exists = specialtyRepository.existsByName("Dermatologist");
        
        assertFalse(exists);
    }

    @Test
    void testExistsByName_CaseSensitive() {
        specialtyRepository.save(testSpecialty);
        
        // JPA queries are case-sensitive by default
        boolean exists = specialtyRepository.existsByName("cardiologist");
        
        assertFalse(exists);
    }

    @Test
    void testFindAll() {
        Specialty spec2 = Specialty.builder()
                .specialtyId("SPEC02")
                .name("Dermatologist")
                .build();
        
        specialtyRepository.save(testSpecialty);
        specialtyRepository.save(spec2);
        
        var allSpecialties = specialtyRepository.findAll();
        
        assertEquals(2, allSpecialties.size());
    }

    @Test
    void testDelete() {
        Specialty saved = specialtyRepository.save(testSpecialty);
        
        specialtyRepository.delete(saved);
        
        Optional<Specialty> found = specialtyRepository.findById("SPEC01");
        assertFalse(found.isPresent());
    }

    @Test
    void testUpdate() {
        Specialty saved = specialtyRepository.save(testSpecialty);
        saved.setName("Updated Cardiologist");
        
        Specialty updated = specialtyRepository.save(saved);
        
        assertEquals("Updated Cardiologist", updated.getName());
        assertEquals("SPEC01", updated.getSpecialtyId());
    }
}

