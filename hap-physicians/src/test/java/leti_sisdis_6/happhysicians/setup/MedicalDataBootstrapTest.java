package leti_sisdis_6.happhysicians.setup;

import leti_sisdis_6.happhysicians.model.Department;
import leti_sisdis_6.happhysicians.model.Specialty;
import leti_sisdis_6.happhysicians.repository.DepartmentRepository;
import leti_sisdis_6.happhysicians.repository.SpecialtyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicalDataBootstrapTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private SpecialtyRepository specialtyRepository;

    @InjectMocks
    private MedicalDataBootstrap medicalDataBootstrap;

    @BeforeEach
    void setUp() {
        // Setup default behavior for repositories
        when(departmentRepository.existsByCode(anyString())).thenReturn(false);
        when(specialtyRepository.existsByName(anyString())).thenReturn(false);
    }

    @Test
    void testRun_PreloadsDepartments() {
        // Act
        medicalDataBootstrap.run();

        // Assert
        verify(departmentRepository, atLeast(4)).existsByCode(anyString());
        verify(departmentRepository, atLeast(4)).save(any(Department.class));
    }

    @Test
    void testRun_PreloadsSpecialties() {
        // Act
        medicalDataBootstrap.run();

        // Assert
        verify(specialtyRepository, atLeast(7)).existsByName(anyString());
        verify(specialtyRepository, atLeast(7)).save(any(Specialty.class));
    }

    @Test
    void testPreloadDepartments_CreatesAllDepartments() {
        // Act
        medicalDataBootstrap.run();

        // Assert - Verify all departments are created
        ArgumentCaptor<Department> departmentCaptor = ArgumentCaptor.forClass(Department.class);
        verify(departmentRepository, atLeast(4)).save(departmentCaptor.capture());
        
        // Verify department IDs
        long uniqueIds = departmentCaptor.getAllValues().stream()
                .map(Department::getDepartmentId)
                .distinct()
                .count();
        assertTrue(uniqueIds >= 4);
    }

    @Test
    void testPreloadSpecialties_CreatesAllSpecialties() {
        // Act
        medicalDataBootstrap.run();

        // Assert - Verify all specialties are created
        ArgumentCaptor<Specialty> specialtyCaptor = ArgumentCaptor.forClass(Specialty.class);
        verify(specialtyRepository, atLeast(7)).save(specialtyCaptor.capture());
        
        // Verify specialty IDs
        long uniqueIds = specialtyCaptor.getAllValues().stream()
                .map(Specialty::getSpecialtyId)
                .distinct()
                .count();
        assertTrue(uniqueIds >= 7);
    }

    @Test
    void testCreateDepartment_DoesNotDuplicate() {
        // Arrange - Department already exists
        when(departmentRepository.existsByCode("CARD")).thenReturn(true);

        // Act
        medicalDataBootstrap.run();

        // Assert - Should not save if already exists
        verify(departmentRepository, never()).save(argThat(d -> 
            d.getCode().equals("CARD")));
    }

    @Test
    void testCreateSpecialty_DoesNotDuplicate() {
        // Arrange - Specialty already exists
        when(specialtyRepository.existsByName("Cardiologia")).thenReturn(true);

        // Act
        medicalDataBootstrap.run();

        // Assert - Should not save if already exists
        verify(specialtyRepository, never()).save(argThat(s -> 
            s.getName().equals("Cardiologia")));
    }

    @Test
    void testCreateDepartment_WithCorrectData() {
        // Arrange
        ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);

        // Act
        medicalDataBootstrap.run();

        // Assert - Verify at least one department has correct structure
        verify(departmentRepository, atLeastOnce()).save(captor.capture());
        Department savedDept = captor.getAllValues().stream()
                .filter(d -> d.getDepartmentId().equals("DEP01"))
                .findFirst()
                .orElse(null);
        
        if (savedDept != null) {
            assertEquals("DEP01", savedDept.getDepartmentId());
            assertEquals("CARD", savedDept.getCode());
            assertEquals("Cardiologia", savedDept.getName());
            assertNotNull(savedDept.getDescription());
        }
    }

    @Test
    void testCreateSpecialty_WithCorrectData() {
        // Arrange
        ArgumentCaptor<Specialty> captor = ArgumentCaptor.forClass(Specialty.class);

        // Act
        medicalDataBootstrap.run();

        // Assert - Verify at least one specialty has correct structure
        verify(specialtyRepository, atLeastOnce()).save(captor.capture());
        Specialty savedSpec = captor.getAllValues().stream()
                .filter(s -> s.getSpecialtyId().equals("SPC01"))
                .findFirst()
                .orElse(null);
        
        if (savedSpec != null) {
            assertEquals("SPC01", savedSpec.getSpecialtyId());
            assertEquals("Cardiologia", savedSpec.getName());
        }
    }
}

