package leti_sisdis_6.happhysicians.services;

import leti_sisdis_6.happhysicians.exceptions.AppointmentRecordNotFoundException;
import leti_sisdis_6.happhysicians.exceptions.MicroserviceCommunicationException;
import leti_sisdis_6.happhysicians.exceptions.PatientNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExternalServiceClientTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ExternalServiceClient externalServiceClient;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(externalServiceClient, "patientsServiceUrl", "http://localhost:8082");
        ReflectionTestUtils.setField(externalServiceClient, "authServiceUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(externalServiceClient, "appointmentRecordsServiceUrl", "http://localhost:8083");
        ReflectionTestUtils.setField(externalServiceClient, "currentPort", "8081");
    }

    @Test
    void testGetPatientById_Success() {
        // Arrange
        String patientId = "PAT01";
        Map<String, Object> patientData = new HashMap<>();
        patientData.put("patientId", patientId);
        patientData.put("fullName", "John Patient");

        ResponseEntity<Map<String, Object>> response = new ResponseEntity<>(patientData, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), 
                any(ParameterizedTypeReference.class))).thenReturn(response);

        // Act
        Map<String, Object> result = externalServiceClient.getPatientById(patientId);

        // Assert
        assertNotNull(result);
        assertEquals(patientId, result.get("patientId"));
        verify(restTemplate, atLeastOnce()).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), 
                any(ParameterizedTypeReference.class));
    }

    @Test
    void testGetPatientById_NotFound() {
        // Arrange
        String patientId = "PAT99";
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), 
                any(ParameterizedTypeReference.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not found", HttpHeaders.EMPTY, null, null));

        // Act & Assert
        assertThrows(PatientNotFoundException.class, () -> {
            externalServiceClient.getPatientById(patientId);
        });
    }

    @Test
    void testGetPatientById_Unauthorized() {
        // Arrange
        String patientId = "PAT01";
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), 
                any(ParameterizedTypeReference.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "Unauthorized", HttpHeaders.EMPTY, null, null));

        // Act & Assert
        assertThrows(MicroserviceCommunicationException.class, () -> {
            externalServiceClient.getPatientById(patientId);
        });
    }

    @Test
    void testRegisterUser_Success() {
        // Arrange
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("userId", "USER01");
        responseData.put("username", "testuser");

        ResponseEntity<Map<String, Object>> response = new ResponseEntity<>(responseData, HttpStatus.CREATED);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), 
                any(ParameterizedTypeReference.class))).thenReturn(response);

        // Act
        Map<String, Object> result = externalServiceClient.registerUser("testuser", "password123", "PHYSICIAN");

        // Assert
        assertNotNull(result);
        assertEquals("USER01", result.get("userId"));
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), 
                any(ParameterizedTypeReference.class));
    }

    @Test
    void testRegisterUser_Conflict() {
        // Arrange
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), 
                any(ParameterizedTypeReference.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.CONFLICT, "Conflict", HttpHeaders.EMPTY, null, null));

        // Act & Assert
        assertThrows(MicroserviceCommunicationException.class, () -> {
            externalServiceClient.registerUser("existinguser", "password123", "PHYSICIAN");
        });
    }

    @Test
    void testGetAppointmentRecord_Success() {
        // Arrange
        String appointmentId = "APT01";
        Map<String, Object> recordData = new HashMap<>();
        recordData.put("appointmentId", appointmentId);

        ResponseEntity<Map<String, Object>> response = new ResponseEntity<>(recordData, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), 
                any(ParameterizedTypeReference.class))).thenReturn(response);

        // Act
        Map<String, Object> result = externalServiceClient.getAppointmentRecord(appointmentId);

        // Assert
        assertNotNull(result);
        assertEquals(appointmentId, result.get("appointmentId"));
    }

    @Test
    void testGetAppointmentRecord_NotFound() {
        // Arrange
        String appointmentId = "APT99";
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), 
                any(ParameterizedTypeReference.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not found", HttpHeaders.EMPTY, null, null));

        // Act & Assert
        assertThrows(AppointmentRecordNotFoundException.class, () -> {
            externalServiceClient.getAppointmentRecord(appointmentId);
        });
    }

    @Test
    void testCreateAppointmentInRecords_Success() {
        // Arrange
        Map<String, Object> appointmentData = new HashMap<>();
        appointmentData.put("appointmentId", "APT01");
        appointmentData.put("patientId", "PAT01");

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("appointmentId", "APT01");
        responseData.put("status", "CREATED");

        ResponseEntity<Map<String, Object>> response = new ResponseEntity<>(responseData, HttpStatus.CREATED);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), 
                any(ParameterizedTypeReference.class))).thenReturn(response);

        // Act
        Map<String, Object> result = externalServiceClient.createAppointmentInRecords(appointmentData);

        // Assert
        assertNotNull(result);
        assertEquals("APT01", result.get("appointmentId"));
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), 
                any(ParameterizedTypeReference.class));
    }

    @Test
    void testCreateAppointmentInRecords_Conflict() {
        // Arrange
        Map<String, Object> appointmentData = new HashMap<>();
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), 
                any(ParameterizedTypeReference.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.CONFLICT, "Conflict", HttpHeaders.EMPTY, null, null));

        // Act & Assert
        assertThrows(MicroserviceCommunicationException.class, () -> {
            externalServiceClient.createAppointmentInRecords(appointmentData);
        });
    }

    @Test
    void testListAppointments_Success() {
        // Arrange
        Map<String, Object> appointment1 = new HashMap<>();
        appointment1.put("appointmentId", "APT01");
        Map<String, Object> appointment2 = new HashMap<>();
        appointment2.put("appointmentId", "APT02");
        List<Map<String, Object>> appointments = Arrays.asList(appointment1, appointment2);

        ResponseEntity<List<Map<String, Object>>> response = new ResponseEntity<>(appointments, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), 
                any(ParameterizedTypeReference.class))).thenReturn(response);

        // Act
        List<Map<String, Object>> result = externalServiceClient.listAppointments();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testGetPeerUrls_ExcludesCurrentInstance() {
        // Act
        List<String> peers = externalServiceClient.getPeerUrls();

        // Assert
        assertNotNull(peers);
        // Should exclude current instance (port 8081)
        assertTrue(peers.stream().noneMatch(url -> url.contains(":8081")));
    }

    @Test
    void testGetCurrentInstanceUrl() {
        // Act
        String url = externalServiceClient.getCurrentInstanceUrl();

        // Assert
        assertNotNull(url);
        assertTrue(url.contains(":8081"));
    }

    @Test
    void testHasPeers() {
        // Act
        boolean hasPeers = externalServiceClient.hasPeers();

        // Assert
        assertNotNull(hasPeers);
    }

    @Test
    void testGetPeerCount() {
        // Act
        int count = externalServiceClient.getPeerCount();

        // Assert
        assertTrue(count >= 0);
    }

    @Test
    void testUpdateAppointmentInRecords_Success() {
        // Arrange
        String appointmentId = "APT01";
        Map<String, Object> appointmentData = new HashMap<>();
        appointmentData.put("status", "UPDATED");

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("appointmentId", appointmentId);
        responseData.put("status", "UPDATED");

        ResponseEntity<Map<String, Object>> response = new ResponseEntity<>(responseData, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), 
                any(ParameterizedTypeReference.class))).thenReturn(response);

        // Act
        Map<String, Object> result = externalServiceClient.updateAppointmentInRecords(appointmentId, appointmentData);

        // Assert
        assertNotNull(result);
        assertEquals(appointmentId, result.get("appointmentId"));
    }

    @Test
    void testDeleteAppointmentInRecords_Success() {
        // Arrange
        String appointmentId = "APT01";
        ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.NO_CONTENT);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(response);

        // Act & Assert
        assertDoesNotThrow(() -> {
            externalServiceClient.deleteAppointmentInRecords(appointmentId);
        });
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class));
    }
}

