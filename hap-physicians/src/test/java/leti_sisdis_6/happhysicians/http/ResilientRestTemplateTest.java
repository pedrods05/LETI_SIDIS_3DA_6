package leti_sisdis_6.happhysicians.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResilientRestTemplateTest {

    @Mock
    private RestTemplate restTemplate;

    private ResilientRestTemplate resilientRestTemplate;

    @BeforeEach
    void setUp() {
        resilientRestTemplate = new ResilientRestTemplate(restTemplate);
    }

    @Test
    void testGetForObjectWithFallback_Success() {
        // Arrange
        String url = "http://localhost:8080/api/test";
        String expectedResponse = "Test Response";
        when(restTemplate.getForObject(url, String.class)).thenReturn(expectedResponse);

        // Act
        String result = resilientRestTemplate.getForObjectWithFallback(url, String.class);

        // Assert
        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(restTemplate, times(1)).getForObject(url, String.class);
    }

    @Test
    void testGetForObjectWithFallback_Exception() {
        // Arrange
        String url = "http://localhost:8080/api/test";
        when(restTemplate.getForObject(url, String.class))
                .thenThrow(new RestClientException("Connection failed"));

        // Act & Assert
        assertThrows(RestClientException.class, () -> {
            resilientRestTemplate.getForObjectWithFallback(url, String.class);
        });

        // Second call should return null (peer marked as failed)
        String result = resilientRestTemplate.getForObjectWithFallback(url, String.class);
        assertNull(result);
    }

    @Test
    void testGetForObjectWithFallback_WithHeaders_Success() {
        // Arrange
        String url = "http://localhost:8081/api/test";
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer token");
        String expectedResponse = "Test Response";

        ResponseEntity<String> responseEntity = new ResponseEntity<>(expectedResponse, HttpStatus.OK);
        when(restTemplate.exchange(eq(url), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);

        // Act
        String result = resilientRestTemplate.getForObjectWithFallback(url, headers, String.class);

        // Assert
        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(restTemplate, times(1)).exchange(eq(url), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void testGetForObjectWithFallback_WithHeaders_Exception() {
        // Arrange
        String url = "http://localhost:8082/api/test";
        HttpHeaders headers = new HttpHeaders();
        when(restTemplate.exchange(eq(url), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("Connection failed"));

        // Act & Assert
        assertThrows(RestClientException.class, () -> {
            resilientRestTemplate.getForObjectWithFallback(url, headers, String.class);
        });

        // Second call should return null (peer marked as failed)
        String result = resilientRestTemplate.getForObjectWithFallback(url, headers, String.class);
        assertNull(result);
    }

    @Test
    void testGetForObjectWithFallback_WithHeaders_NullHeaders() {
        // Arrange
        String url = "http://localhost:8083/api/test";
        String expectedResponse = "Test Response";

        ResponseEntity<String> responseEntity = new ResponseEntity<>(expectedResponse, HttpStatus.OK);
        when(restTemplate.exchange(eq(url), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);

        // Act
        String result = resilientRestTemplate.getForObjectWithFallback(url, null, String.class);

        // Assert
        assertNotNull(result);
        assertEquals(expectedResponse, result);
    }

    @Test
    void testMultiplePeerFailures() {
        // Arrange
        String url1 = "http://localhost:8080/api/test";
        String url2 = "http://localhost:8081/api/test";
        
        when(restTemplate.getForObject(url1, String.class))
                .thenThrow(new RestClientException("Connection failed"));
        when(restTemplate.getForObject(url2, String.class))
                .thenReturn("Success");

        // Act & Assert - First URL fails
        assertThrows(RestClientException.class, () -> {
            resilientRestTemplate.getForObjectWithFallback(url1, String.class);
        });

        // Second URL should still work
        String result = resilientRestTemplate.getForObjectWithFallback(url2, String.class);
        assertEquals("Success", result);

        // First URL should return null (marked as failed)
        String failedResult = resilientRestTemplate.getForObjectWithFallback(url1, String.class);
        assertNull(failedResult);
    }

    @Test
    void testConstructor_WithRestTemplate() {
        // Arrange
        RestTemplate customRestTemplate = new RestTemplate();

        // Act
        ResilientRestTemplate custom = new ResilientRestTemplate(customRestTemplate);

        // Assert
        assertNotNull(custom);
    }

    @Test
    void testConstructor_WithoutRestTemplate() {
        // Act
        ResilientRestTemplate defaultTemplate = new ResilientRestTemplate();

        // Assert
        assertNotNull(defaultTemplate);
    }
}

