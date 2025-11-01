package leti_sisdis_6.happhysicians.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DepartmentTest {

    private Department department;

    @BeforeEach
    void setUp() {
        department = new Department();
    }

    @Test
    void testNoArgsConstructor() {
        assertNotNull(department);
        assertNull(department.getDepartmentId());
        assertNull(department.getCode());
        assertNull(department.getName());
        assertNull(department.getDescription());
    }

    @Test
    void testAllArgsConstructor() {
        Department dept = new Department("DEPT01", "CARD", "Cardiology", "Heart and cardiovascular diseases");
        
        assertEquals("DEPT01", dept.getDepartmentId());
        assertEquals("CARD", dept.getCode());
        assertEquals("Cardiology", dept.getName());
        assertEquals("Heart and cardiovascular diseases", dept.getDescription());
    }

    @Test
    void testBuilder() {
        Department dept = Department.builder()
                .departmentId("DEPT02")
                .code("DERM")
                .name("Dermatology")
                .description("Skin diseases")
                .build();

        assertEquals("DEPT02", dept.getDepartmentId());
        assertEquals("DERM", dept.getCode());
        assertEquals("Dermatology", dept.getName());
        assertEquals("Skin diseases", dept.getDescription());
    }

    @Test
    void testSettersAndGetters() {
        department.setDepartmentId("DEPT03");
        department.setCode("NEURO");
        department.setName("Neurology");
        department.setDescription("Brain and nervous system");

        assertEquals("DEPT03", department.getDepartmentId());
        assertEquals("NEURO", department.getCode());
        assertEquals("Neurology", department.getName());
        assertEquals("Brain and nervous system", department.getDescription());
    }

    @Test
    void testBuilder_WithNullValues() {
        Department dept = Department.builder()
                .departmentId("DEPT04")
                .code("ORTH")
                .name("Orthopedics")
                .build(); // description is null

        assertEquals("DEPT04", dept.getDepartmentId());
        assertEquals("ORTH", dept.getCode());
        assertEquals("Orthopedics", dept.getName());
        assertNull(dept.getDescription());
    }
}

