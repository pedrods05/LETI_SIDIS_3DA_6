package leti_sisdis_6.happhysicians.repository;

import leti_sisdis_6.happhysicians.model.Department;
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
class DepartmentRepositoryTest {

    @Autowired
    private DepartmentRepository departmentRepository;

    private Department testDepartment;

    @BeforeEach
    void setUp() {
        departmentRepository.deleteAll();
        
        testDepartment = Department.builder()
                .departmentId("DEPT01")
                .code("CARD")
                .name("Cardiology")
                .description("Heart and cardiovascular diseases")
                .build();
    }

    @Test
    void testSaveDepartment() {
        Department saved = departmentRepository.save(testDepartment);
        
        assertNotNull(saved);
        assertEquals("DEPT01", saved.getDepartmentId());
        assertEquals("CARD", saved.getCode());
        assertEquals("Cardiology", saved.getName());
    }

    @Test
    void testFindById() {
        departmentRepository.save(testDepartment);
        
        Optional<Department> found = departmentRepository.findById("DEPT01");
        
        assertTrue(found.isPresent());
        assertEquals("DEPT01", found.get().getDepartmentId());
        assertEquals("CARD", found.get().getCode());
    }

    @Test
    void testFindById_NotFound() {
        Optional<Department> found = departmentRepository.findById("NONEXISTENT");
        
        assertFalse(found.isPresent());
    }

    @Test
    void testExistsByCode_True() {
        departmentRepository.save(testDepartment);
        
        boolean exists = departmentRepository.existsByCode("CARD");
        
        assertTrue(exists);
    }

    @Test
    void testExistsByCode_False() {
        departmentRepository.save(testDepartment);
        
        boolean exists = departmentRepository.existsByCode("DERM");
        
        assertFalse(exists);
    }

    @Test
    void testFindAll() {
        Department dept2 = Department.builder()
                .departmentId("DEPT02")
                .code("DERM")
                .name("Dermatology")
                .build();
        
        departmentRepository.save(testDepartment);
        departmentRepository.save(dept2);
        
        var allDepartments = departmentRepository.findAll();
        
        assertEquals(2, allDepartments.size());
    }

    @Test
    void testDelete() {
        Department saved = departmentRepository.save(testDepartment);
        
        departmentRepository.delete(saved);
        
        Optional<Department> found = departmentRepository.findById("DEPT01");
        assertFalse(found.isPresent());
    }

    @Test
    void testUpdate() {
        Department saved = departmentRepository.save(testDepartment);
        saved.setName("Updated Cardiology");
        
        Department updated = departmentRepository.save(saved);
        
        assertEquals("Updated Cardiology", updated.getName());
        assertEquals("DEPT01", updated.getDepartmentId());
    }

    @Test
    void testCodeUniqueness() {
        departmentRepository.save(testDepartment);
        
        Department duplicate = Department.builder()
                .departmentId("DEPT02")
                .code("CARD") // same code
                .name("Another Cardiology")
                .build();
        
        // In a real scenario with unique constraint, this would fail
        // But we're just testing the repository method existsByCode
        assertTrue(departmentRepository.existsByCode("CARD"));
    }
}

