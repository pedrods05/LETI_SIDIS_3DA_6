package leti_sisdis_6.happhysicians.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpecialtyTest {

    private Specialty specialty;

    @BeforeEach
    void setUp() {
        specialty = new Specialty();
    }

    @Test
    void testNoArgsConstructor() {
        assertNotNull(specialty);
        assertNull(specialty.getSpecialtyId());
        assertNull(specialty.getName());
    }

    @Test
    void testAllArgsConstructor() {
        Specialty spec = new Specialty("SPEC01", "Cardiologist");
        
        assertEquals("SPEC01", spec.getSpecialtyId());
        assertEquals("Cardiologist", spec.getName());
    }

    @Test
    void testBuilder() {
        Specialty spec = Specialty.builder()
                .specialtyId("SPEC02")
                .name("Dermatologist")
                .build();

        assertEquals("SPEC02", spec.getSpecialtyId());
        assertEquals("Dermatologist", spec.getName());
    }

    @Test
    void testSettersAndGetters() {
        specialty.setSpecialtyId("SPEC03");
        specialty.setName("Neurologist");

        assertEquals("SPEC03", specialty.getSpecialtyId());
        assertEquals("Neurologist", specialty.getName());
    }

    @Test
    void testBuilder_MinimalValues() {
        Specialty spec = Specialty.builder()
                .specialtyId("SPEC04")
                .name("General Practitioner")
                .build();

        assertEquals("SPEC04", spec.getSpecialtyId());
        assertEquals("General Practitioner", spec.getName());
    }
}

