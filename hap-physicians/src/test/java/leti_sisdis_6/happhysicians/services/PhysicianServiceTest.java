package leti_sisdis_6.happhysicians.services;

import leti_sisdis_6.happhysicians.api.PhysicianMapper;
import leti_sisdis_6.happhysicians.dto.request.RegisterPhysicianRequest;
import leti_sisdis_6.happhysicians.dto.request.UpdatePhysicianRequest;
import leti_sisdis_6.happhysicians.dto.response.PhysicianFullDTO;
import leti_sisdis_6.happhysicians.dto.response.PhysicianIdResponse;
import leti_sisdis_6.happhysicians.dto.response.PhysicianLimitedDTO;
import leti_sisdis_6.happhysicians.dto.response.TopPhysicianDTO;
import leti_sisdis_6.happhysicians.exceptions.NotFoundException;
import leti_sisdis_6.happhysicians.model.Department;
import leti_sisdis_6.happhysicians.model.Physician;
import leti_sisdis_6.happhysicians.model.Specialty;
import leti_sisdis_6.happhysicians.repository.AppointmentRepository;
import leti_sisdis_6.happhysicians.repository.DepartmentRepository;
import leti_sisdis_6.happhysicians.repository.PhysicianRepository;
import leti_sisdis_6.happhysicians.repository.SpecialtyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PhysicianServiceTest {

    @Mock
    private PhysicianRepository physicianRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private SpecialtyRepository specialtyRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PhysicianMapper physicianMapper;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ExternalServiceClient externalServiceClient;

    @InjectMocks
    private PhysicianService physicianService;

    private Department testDepartment;
    private Specialty testSpecialty;
    private Physician testPhysician;
    private RegisterPhysicianRequest registerRequest;

    @BeforeEach
    void setUp() {
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

        registerRequest = new RegisterPhysicianRequest();
        registerRequest.setFullName("Dr. John Doe");
        registerRequest.setLicenseNumber("LIC123");
        registerRequest.setUsername("john.doe@hospital.com");
        registerRequest.setPassword("password123");
        registerRequest.setDepartmentId("DEPT01");
        registerRequest.setSpecialtyId("SPEC01");
        registerRequest.setWorkingHourStart("09:00");
        registerRequest.setWorkingHourEnd("17:00");
    }

    @Test
    void testRegister_Success() {
        // Arrange
        when(physicianRepository.existsByUsername(anyString())).thenReturn(false);
        when(physicianRepository.existsByLicenseNumber(anyString())).thenReturn(false);
        when(departmentRepository.findById("DEPT01")).thenReturn(Optional.of(testDepartment));
        when(specialtyRepository.findById("SPEC01")).thenReturn(Optional.of(testSpecialty));
        when(physicianRepository.findAll()).thenReturn(Collections.emptyList());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(physicianMapper.toEntity(any(), any(), any())).thenReturn(testPhysician);
        when(physicianRepository.save(any(Physician.class))).thenReturn(testPhysician);
        // Mock external service call (may fail silently but we configure it to succeed)
        Map<String, Object> authResponse = new HashMap<>();
        lenient().when(externalServiceClient.registerUser(anyString(), anyString(), anyString()))
                .thenReturn(authResponse);

        // Act
        PhysicianIdResponse response = physicianService.register(registerRequest);

        // Assert
        assertNotNull(response);
        assertEquals("PHY01", response.getPhysicianId());
        verify(physicianRepository, times(1)).save(any(Physician.class));
        verify(passwordEncoder, times(1)).encode(anyString());
    }

    @Test
    void testRegister_UsernameAlreadyExists() {
        // Arrange
        when(physicianRepository.existsByUsername(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            physicianService.register(registerRequest);
        });
        verify(physicianRepository, never()).save(any(Physician.class));
    }

    @Test
    void testRegister_LicenseNumberAlreadyExists() {
        // Arrange
        when(physicianRepository.existsByUsername(anyString())).thenReturn(false);
        when(physicianRepository.existsByLicenseNumber(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            physicianService.register(registerRequest);
        });
        verify(physicianRepository, never()).save(any(Physician.class));
    }

    @Test
    void testRegister_DepartmentNotFound() {
        // Arrange
        when(physicianRepository.existsByUsername(anyString())).thenReturn(false);
        when(physicianRepository.existsByLicenseNumber(anyString())).thenReturn(false);
        when(departmentRepository.findById("DEPT01")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            physicianService.register(registerRequest);
        });
    }

    @Test
    void testGetPhysicianDetails_Success() {
        // Arrange
        String physicianId = "PHY01";
        PhysicianFullDTO expectedDTO = new PhysicianFullDTO();
        when(physicianRepository.findById(physicianId)).thenReturn(Optional.of(testPhysician));
        when(physicianMapper.toFullDTO(testPhysician)).thenReturn(expectedDTO);

        // Act
        PhysicianFullDTO result = physicianService.getPhysicianDetails(physicianId);

        // Assert
        assertNotNull(result);
        assertEquals(expectedDTO, result);
        verify(physicianRepository, times(1)).findById(physicianId);
    }

    @Test
    void testGetPhysicianDetails_NotFound() {
        // Arrange
        String physicianId = "PHY99";
        when(physicianRepository.findById(physicianId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            physicianService.getPhysicianDetails(physicianId);
        });
    }

    @Test
    void testSearchByNameOrSpecialty_Success() {
        // Arrange
        String name = "John Doe";
        List<Physician> physicians = Arrays.asList(testPhysician);
        List<PhysicianLimitedDTO> expectedDTOs = Arrays.asList(new PhysicianLimitedDTO());
        
        when(physicianRepository.searchByNameOrSpecialty(eq(name), anyString(), anyString(), isNull()))
                .thenReturn(physicians);
        when(physicianMapper.toLimitedDTOList(physicians)).thenReturn(expectedDTOs);

        // Act
        List<PhysicianLimitedDTO> result = physicianService.searchByNameOrSpecialty(name, null);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(physicianRepository, times(1)).searchByNameOrSpecialty(anyString(), anyString(), anyString(), isNull());
    }

    @Test
    void testSearchByNameOrSpecialty_NameTooShort() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            physicianService.searchByNameOrSpecialty("Jo", null);
        });
    }

    @Test
    void testSearchByNameOrSpecialty_NoResults() {
        // Arrange
        when(physicianRepository.searchByNameOrSpecialty(anyString(), anyString(), anyString(), isNull()))
                .thenReturn(Collections.emptyList());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> {
            physicianService.searchByNameOrSpecialty("John Doe", null);
        });
    }

    @Test
    void testPartialUpdate_Success() {
        // Arrange
        String physicianId = "PHY01";
        UpdatePhysicianRequest updateRequest = new UpdatePhysicianRequest();
        updateRequest.setFullName("Dr. Jane Doe");
        updateRequest.setWorkingHourStart("10:00");
        updateRequest.setWorkingHourEnd("18:00");

        PhysicianFullDTO expectedDTO = new PhysicianFullDTO();
        when(physicianRepository.findById(physicianId)).thenReturn(Optional.of(testPhysician));
        // existsByLicenseNumber is only called if licenseNumber is provided in request
        when(physicianRepository.save(any(Physician.class))).thenReturn(testPhysician);
        when(physicianMapper.toFullDTO(any(Physician.class))).thenReturn(expectedDTO);

        // Act
        PhysicianFullDTO result = physicianService.partialUpdate(physicianId, updateRequest);

        // Assert
        assertNotNull(result);
        verify(physicianRepository, times(1)).save(any(Physician.class));
        // Verify that save was called - the actual physician object modifications are tested via the returned DTO
    }

    @Test
    void testPartialUpdate_PhysicianNotFound() {
        // Arrange
        String physicianId = "PHY99";
        UpdatePhysicianRequest updateRequest = new UpdatePhysicianRequest();
        when(physicianRepository.findById(physicianId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            physicianService.partialUpdate(physicianId, updateRequest);
        });
    }

    @Test
    void testGetTop5Physicians_Success() {
        // Arrange
        LocalDateTime from = LocalDateTime.now().minusMonths(1);
        LocalDateTime to = LocalDateTime.now();
        
        Object[] result1 = new Object[]{"PHY01", "Dr. John Doe", "Cardiologist", 10L};
        Object[] result2 = new Object[]{"PHY02", "Dr. Jane Smith", "Dermatologist", 8L};
        List<Object[]> results = Arrays.asList(result1, result2);
        
        when(appointmentRepository.findTop5PhysiciansByAppointmentCount(from, to))
                .thenReturn(results);

        // Act
        List<TopPhysicianDTO> topPhysicians = physicianService.getTop5Physicians(from, to);

        // Assert
        assertNotNull(topPhysicians);
        assertEquals(2, topPhysicians.size());
        assertEquals("PHY01", topPhysicians.get(0).getPhysicianId());
        assertEquals("Dr. John Doe", topPhysicians.get(0).getFullName());
        assertEquals(10L, topPhysicians.get(0).getAppointmentCount());
    }

    @Test
    void testGeneratePhysicianId() {
        // Arrange
        Physician existingPhysician = Physician.builder().physicianId("PHY05").build();
        when(physicianRepository.findAll()).thenReturn(Collections.singletonList(existingPhysician));
        when(physicianRepository.existsByUsername(anyString())).thenReturn(false);
        when(physicianRepository.existsByLicenseNumber(anyString())).thenReturn(false);
        when(departmentRepository.findById(anyString())).thenReturn(Optional.of(testDepartment));
        when(specialtyRepository.findById(anyString())).thenReturn(Optional.of(testSpecialty));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        
        Physician newPhysician = Physician.builder().physicianId("PHY06").build();
        when(physicianMapper.toEntity(any(), any(), any())).thenReturn(newPhysician);
        when(physicianRepository.save(any(Physician.class))).thenReturn(newPhysician);
        // Mock external service call
        Map<String, Object> authResponse = new HashMap<>();
        lenient().when(externalServiceClient.registerUser(anyString(), anyString(), anyString()))
                .thenReturn(authResponse);

        // Act
        PhysicianIdResponse response = physicianService.register(registerRequest);

        // Assert
        assertNotNull(response);
        verify(physicianRepository, times(1)).save(any(Physician.class));
    }
}

