package leti_sisdis_6.happhysicians.setup;

import leti_sisdis_6.happhysicians.model.Department;
import leti_sisdis_6.happhysicians.model.Physician;
import leti_sisdis_6.happhysicians.model.Specialty;
import leti_sisdis_6.happhysicians.repository.DepartmentRepository;
import leti_sisdis_6.happhysicians.repository.PhysicianRepository;
import leti_sisdis_6.happhysicians.repository.SpecialtyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PhysiciansDataBootstrapTest {

    @Mock
    private PhysicianRepository physicianRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private SpecialtyRepository specialtyRepository;

    @InjectMocks
    private PhysiciansDataBootstrap physiciansDataBootstrap;

    private Department testDepartment1;
    private Department testDepartment2;
    private Specialty testSpecialty1;
    private Specialty testSpecialty2;

    @BeforeEach
    void setUp() {
        // Setup test departments
        testDepartment1 = Department.builder()
                .departmentId("DEP01")
                .code("CARD")
                .name("Cardiologia")
                .build();

        testDepartment2 = Department.builder()
                .departmentId("DEP02")
                .code("NEUR")
                .name("Neurologia")
                .build();

        // Setup test specialties
        testSpecialty1 = Specialty.builder()
                .specialtyId("SPC01")
                .name("Cardiologia")
                .build();

        testSpecialty2 = Specialty.builder()
                .specialtyId("SPC02")
                .name("Neurologia")
                .build();

        // Mock repository responses
        when(departmentRepository.findById("DEP01")).thenReturn(Optional.of(testDepartment1));
        when(departmentRepository.findById("DEP02")).thenReturn(Optional.of(testDepartment2));
        when(departmentRepository.findById("DEP03")).thenReturn(Optional.of(testDepartment1));
        when(departmentRepository.findById("DEP04")).thenReturn(Optional.of(testDepartment2));

        when(specialtyRepository.findById("SPC01")).thenReturn(Optional.of(testSpecialty1));
        when(specialtyRepository.findById("SPC02")).thenReturn(Optional.of(testSpecialty2));
        when(specialtyRepository.findById("SPC03")).thenReturn(Optional.of(testSpecialty1));
        when(specialtyRepository.findById("SPC04")).thenReturn(Optional.of(testSpecialty1));
        when(specialtyRepository.findById("SPC05")).thenReturn(Optional.of(testSpecialty1));
        when(specialtyRepository.findById("SPC06")).thenReturn(Optional.of(testSpecialty1));
        when(specialtyRepository.findById("SPC07")).thenReturn(Optional.of(testSpecialty1));
    }

    @Test
    void testRun_WhenRepositoryIsEmpty_CreatesPhysicians() {
        // Arrange
        when(physicianRepository.count()).thenReturn(0L);

        // Act
        physiciansDataBootstrap.run();

        // Assert
        verify(physicianRepository, times(1)).saveAll(any(List.class));
    }

    @Test
    void testRun_WhenRepositoryHasData_SkipsCreation() {
        // Arrange
        when(physicianRepository.count()).thenReturn(5L);

        // Act
        physiciansDataBootstrap.run();

        // Assert
        verify(physicianRepository, never()).saveAll(any(List.class));
    }

    @Test
    void testRun_CreatesPhysiciansWithCorrectData() {
        // Arrange
        when(physicianRepository.count()).thenReturn(0L);
        ArgumentCaptor<List<Physician>> captor = ArgumentCaptor.forClass(List.class);

        // Act
        physiciansDataBootstrap.run();

        // Assert
        verify(physicianRepository, times(1)).saveAll(captor.capture());
        List<Physician> savedPhysicians = captor.getValue();
        
        assertNotNull(savedPhysicians);
        assertTrue(savedPhysicians.size() >= 2);
        
        // Verify first physician
        Physician phy1 = savedPhysicians.stream()
                .filter(p -> p.getPhysicianId().equals("PHY01"))
                .findFirst()
                .orElse(null);
        
        assertNotNull(phy1);
        assertEquals("Dr. João Silva", phy1.getFullName());
        assertEquals("12345", phy1.getLicenseNumber());
        assertEquals(testDepartment1, phy1.getDepartment());
        assertEquals(testSpecialty1, phy1.getSpecialty());
        assertEquals(LocalTime.of(9, 0), phy1.getWorkingHourStart());
        assertEquals(LocalTime.of(17, 0), phy1.getWorkingHourEnd());
    }

    @Test
    void testRun_CreatesSecondPhysician() {
        // Arrange
        when(physicianRepository.count()).thenReturn(0L);
        ArgumentCaptor<List<Physician>> captor = ArgumentCaptor.forClass(List.class);

        // Act
        physiciansDataBootstrap.run();

        // Assert
        verify(physicianRepository, times(1)).saveAll(captor.capture());
        List<Physician> savedPhysicians = captor.getValue();
        
        // Verify second physician
        Physician phy2 = savedPhysicians.stream()
                .filter(p -> p.getPhysicianId().equals("PHY02"))
                .findFirst()
                .orElse(null);
        
        assertNotNull(phy2);
        assertEquals("Dra. Maria Santos", phy2.getFullName());
        assertEquals("67890", phy2.getLicenseNumber());
        assertEquals(LocalTime.of(8, 0), phy2.getWorkingHourStart());
        assertEquals(LocalTime.of(16, 0), phy2.getWorkingHourEnd());
    }

    @Test
    void testRun_ThrowsExceptionWhenDepartmentNotFound() {
        // Arrange
        when(physicianRepository.count()).thenReturn(0L);
        when(departmentRepository.findById("DEP01")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(Exception.class, () -> {
            physiciansDataBootstrap.run();
        });
    }

    @Test
    void testRun_ThrowsExceptionWhenSpecialtyNotFound() {
        // Arrange
        when(physicianRepository.count()).thenReturn(0L);
        when(specialtyRepository.findById("SPC01")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(Exception.class, () -> {
            physiciansDataBootstrap.run();
        });
    }

    @Test
    void testRun_PhysiciansHaveRequiredFields() {
        // Arrange
        when(physicianRepository.count()).thenReturn(0L);
        ArgumentCaptor<List<Physician>> captor = ArgumentCaptor.forClass(List.class);

        // Act
        physiciansDataBootstrap.run();

        // Assert
        verify(physicianRepository, times(1)).saveAll(captor.capture());
        List<Physician> savedPhysicians = captor.getValue();
        
        for (Physician physician : savedPhysicians) {
            assertNotNull(physician.getPhysicianId());
            assertNotNull(physician.getFullName());
            assertNotNull(physician.getLicenseNumber());
            assertNotNull(physician.getDepartment());
            assertNotNull(physician.getSpecialty());
            assertNotNull(physician.getWorkingHourStart());
            assertNotNull(physician.getWorkingHourEnd());
            assertNotNull(physician.getUsername());
            assertNotNull(physician.getPassword());
        }
    }
}

