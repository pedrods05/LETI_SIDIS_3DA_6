package leti_sisdis_6.happhysicians.api;

import leti_sisdis_6.happhysicians.dto.request.RegisterPhysicianRequest;
import leti_sisdis_6.happhysicians.dto.response.PhysicianIdResponse;
import leti_sisdis_6.happhysicians.model.Department;
import leti_sisdis_6.happhysicians.model.Physician;
import leti_sisdis_6.happhysicians.model.Specialty;
import leti_sisdis_6.happhysicians.repository.PhysicianRepository;
import leti_sisdis_6.happhysicians.services.ExternalServiceClient;
import leti_sisdis_6.happhysicians.services.PhysicianService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhysicianControllerTest {

    @Mock
    private PhysicianRepository physicianRepository;

    @Mock
    private PhysicianService physicianService;

    @Mock
    private ExternalServiceClient externalServiceClient;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private PhysicianController physicianController;

    private Physician testPhysician;
    private Department testDepartment;
    private Specialty testSpecialty;

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
    }

    @Test
    void testGetPhysician_Success() {
        // Arrange
        String physicianId = "PHY01";
        when(physicianRepository.findById(physicianId)).thenReturn(Optional.of(testPhysician));

        // Act
        ResponseEntity<Physician> response = physicianController.getPhysician(physicianId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(physicianId, response.getBody().getPhysicianId());
        verify(physicianRepository, times(1)).findById(physicianId);
    }

    @Test
    void testGetPhysician_NotFound() {
        // Arrange
        String physicianId = "PHY99";
        when(physicianRepository.findById(physicianId)).thenReturn(Optional.empty());
        when(externalServiceClient.getPeerUrls()).thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<Physician> response = physicianController.getPhysician(physicianId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(physicianRepository, times(1)).findById(physicianId);
    }

    @Test
    void testGetPhysician_FromPeer() {
        // Arrange
        String physicianId = "PHY01";
        String peerUrl = "http://peer1:8080";
        Physician peerPhysician = Physician.builder()
                .physicianId(physicianId)
                .fullName("Dr. Peer Physician")
                .build();

        when(physicianRepository.findById(physicianId)).thenReturn(Optional.empty());
        when(externalServiceClient.getPeerUrls()).thenReturn(Collections.singletonList(peerUrl));
        when(restTemplate.getForObject(eq(peerUrl + "/internal/physicians/" + physicianId), eq(Physician.class)))
                .thenReturn(peerPhysician);

        // Act
        ResponseEntity<Physician> response = physicianController.getPhysician(physicianId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(physicianId, response.getBody().getPhysicianId());
        verify(restTemplate, times(1)).getForObject(anyString(), eq(Physician.class));
    }

    @Test
    void testRegisterPhysician_Success() {
        // Arrange
        RegisterPhysicianRequest request = new RegisterPhysicianRequest();
        request.setFullName("Dr. New Physician");
        request.setLicenseNumber("LIC999");
        request.setUsername("new@hospital.com");
        request.setPassword("password123");
        request.setDepartmentId("DEPT01");
        request.setSpecialtyId("SPEC01");

        PhysicianIdResponse response = new PhysicianIdResponse("PHY02", "Success", null);
        when(physicianService.register(request)).thenReturn(response);

        // Act
        ResponseEntity<?> result = physicianController.registerPhysician(request);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(physicianService, times(1)).register(request);
    }

    @Test
    void testRegisterPhysician_IllegalArgument() {
        // Arrange
        RegisterPhysicianRequest request = new RegisterPhysicianRequest();
        when(physicianService.register(request))
                .thenThrow(new IllegalArgumentException("Username already in use"));

        // Act
        ResponseEntity<?> result = physicianController.registerPhysician(request);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        verify(physicianService, times(1)).register(request);
    }

    @Test
    void testGetAllPhysicians_Success() {
        // Arrange
        List<Physician> physicians = Arrays.asList(testPhysician);
        when(physicianRepository.findAll()).thenReturn(physicians);

        // Act
        List<Physician> result = physicianController.getAllPhysicians();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("PHY01", result.get(0).getPhysicianId());
        verify(physicianRepository, times(1)).findAll();
    }

    @Test
    void testUpdatePhysician_Success() {
        // Arrange
        String physicianId = "PHY01";
        Physician updatedDetails = Physician.builder()
                .fullName("Dr. Updated Name")
                .licenseNumber("LIC456")
                .build();

        when(physicianRepository.findById(physicianId)).thenReturn(Optional.of(testPhysician));
        when(physicianRepository.save(any(Physician.class))).thenReturn(testPhysician);

        // Act
        ResponseEntity<Physician> response = physicianController.updatePhysician(physicianId, updatedDetails);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(physicianRepository, times(1)).save(any(Physician.class));
    }

    @Test
    void testUpdatePhysician_NotFound() {
        // Arrange
        String physicianId = "PHY99";
        Physician updatedDetails = new Physician();
        when(physicianRepository.findById(physicianId)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<Physician> response = physicianController.updatePhysician(physicianId, updatedDetails);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(physicianRepository, never()).save(any(Physician.class));
    }

    @Test
    void testDeletePhysician_Success() {
        // Arrange
        String physicianId = "PHY01";
        when(physicianRepository.existsById(physicianId)).thenReturn(true);
        doNothing().when(physicianRepository).deleteById(physicianId);

        // Act
        ResponseEntity<Void> response = physicianController.deletePhysician(physicianId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(physicianRepository, times(1)).deleteById(physicianId);
    }

    @Test
    void testDeletePhysician_NotFound() {
        // Arrange
        String physicianId = "PHY99";
        when(physicianRepository.existsById(physicianId)).thenReturn(false);

        // Act
        ResponseEntity<Void> response = physicianController.deletePhysician(physicianId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(physicianRepository, never()).deleteById(anyString());
    }

    @Test
    void testGetPhysiciansBySpecialty_Success() {
        // Arrange
        String specialtyId = "SPEC01";
        List<Physician> physicians = Arrays.asList(testPhysician);
        when(physicianRepository.findBySpecialtySpecialtyId(specialtyId)).thenReturn(physicians);

        // Act
        List<Physician> result = physicianController.getPhysiciansBySpecialty(specialtyId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(physicianRepository, times(1)).findBySpecialtySpecialtyId(specialtyId);
    }

    @Test
    void testGetPhysiciansByDepartment_Success() {
        // Arrange
        String departmentId = "DEPT01";
        List<Physician> physicians = Arrays.asList(testPhysician);
        when(physicianRepository.findByDepartmentDepartmentId(departmentId)).thenReturn(physicians);

        // Act
        List<Physician> result = physicianController.getPhysiciansByDepartment(departmentId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(physicianRepository, times(1)).findByDepartmentDepartmentId(departmentId);
    }
}

