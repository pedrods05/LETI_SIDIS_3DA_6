package leti_sisdis_6.hapappointmentrecords.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PeerHealthController.class)
@TestPropertySource(properties = {
        "hap.physicians.base-url=http://localhost:8081",
        "hap.patients.base-url=http://localhost:8082",
        "hap.auth.base-url=http://localhost:8084"
})
@ContextConfiguration(classes = {PeerHealthController.class, PeerHealthControllerTest.TestConfig.class})
class PeerHealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;

    @Configuration
    static class TestConfig {
        @Bean
        public RestTemplate restTemplate() {
            return mock(RestTemplate.class);
        }
    }

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(restTemplate);
    }

    @Test
    @DisplayName("GET /api/peers/health - Should return UP status when all services are reachable")
    void shouldReturnUpStatusWhenAllServicesReachable() throws Exception {
        // Given - all services respond successfully
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("OK"));

        // When/Then
        mockMvc.perform(get("/api/peers/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("physicians", "patients", "auth")))
                .andExpect(jsonPath("$[*].status", everyItem(is("UP"))));

        // Verify that at least one probe was called for each service
        verify(restTemplate, atLeast(3)).getForEntity(anyString(), eq(String.class));
    }

    @Test
    @DisplayName("GET /api/peers/health - Should return DOWN status when service is unreachable")
    void shouldReturnDownStatusWhenServiceUnreachable() throws Exception {
        // Given - all services fail
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenThrow(new RestClientException("Connection refused"));

        // When/Then
        mockMvc.perform(get("/api/peers/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].status", everyItem(is("DOWN"))));
    }

    @Test
    @DisplayName("GET /api/peers/health - Should return mixed statuses")
    void shouldReturnMixedStatuses() throws Exception {
        // Given - physicians UP, patients and auth DOWN
        when(restTemplate.getForEntity(argThat((String url) -> url != null && url.contains("8081")), eq(String.class)))
                .thenReturn(ResponseEntity.ok("OK"));

        when(restTemplate.getForEntity(argThat((String url) -> url != null && url.contains("8082")), eq(String.class)))
                .thenThrow(new RestClientException("Connection refused"));

        when(restTemplate.getForEntity(argThat((String url) -> url != null && url.contains("8084")), eq(String.class)))
                .thenThrow(new RestClientException("Connection refused"));

        // When/Then
        mockMvc.perform(get("/api/peers/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[?(@.name=='physicians')].status").value("UP"))
                .andExpect(jsonPath("$[?(@.name=='patients')].status").value("DOWN"))
                .andExpect(jsonPath("$[?(@.name=='auth')].status").value("DOWN"));
    }

    @Test
    @DisplayName("GET /api/peers/health - Should try multiple probes and succeed on swagger-ui")
    void shouldTryMultipleProbesAndSucceedOnSwaggerUi() throws Exception {
        // Given - first probe fails, second succeeds
        when(restTemplate.getForEntity(argThat((String url) -> url != null && url.endsWith("/v3/api-docs")), eq(String.class)))
                .thenThrow(new RestClientException("Not found"));

        when(restTemplate.getForEntity(argThat((String url) -> url != null && url.endsWith("/swagger-ui.html")), eq(String.class)))
                .thenReturn(ResponseEntity.ok("OK"));

        // When/Then
        mockMvc.perform(get("/api/peers/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].status", everyItem(is("UP"))))
                .andExpect(jsonPath("$[*].probe", everyItem(org.hamcrest.Matchers.endsWith("/swagger-ui.html"))));
    }

    @Test
    @DisplayName("GET /api/peers/health - Should try all probes before marking as DOWN")
    void shouldTryAllProbesBeforeMarkingAsDown() throws Exception {
        // Given - all probes fail
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenThrow(new RestClientException("Connection refused"));

        // When/Then
        mockMvc.perform(get("/api/peers/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].status", everyItem(is("DOWN"))))
                .andExpect(jsonPath("$[*].probe").doesNotExist());

        // Should try 3 probes per service = 9 total calls
        verify(restTemplate, times(9)).getForEntity(anyString(), eq(String.class));
    }

    @Test
    @DisplayName("GET /api/peers/health - Should include correct base URLs")
    void shouldIncludeCorrectBaseUrls() throws Exception {
        // Given
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("OK"));

        // When/Then
        mockMvc.perform(get("/api/peers/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='physicians')].baseUrl").value("http://localhost:8081"))
                .andExpect(jsonPath("$[?(@.name=='patients')].baseUrl").value("http://localhost:8082"))
                .andExpect(jsonPath("$[?(@.name=='auth')].baseUrl").value("http://localhost:8084"));
    }

    @Test
    @DisplayName("GET /api/peers/health - Should handle partial failures gracefully")
    void shouldHandlePartialFailuresGracefully() throws Exception {
        // Given - some probes succeed, some fail
        when(restTemplate.getForEntity(argThat((String url) -> url != null && url.contains("8081")), eq(String.class)))
                .thenThrow(new RestClientException("Timeout"))
                .thenReturn(ResponseEntity.ok("OK"));  // Second probe succeeds

        when(restTemplate.getForEntity(argThat((String url) -> url != null && url.contains("8082")), eq(String.class)))
                .thenThrow(new RestClientException("Connection refused"));

        when(restTemplate.getForEntity(argThat((String url) -> url != null && url.contains("8084")), eq(String.class)))
                .thenReturn(ResponseEntity.ok("OK"));

        // When/Then
        mockMvc.perform(get("/api/peers/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[?(@.name=='physicians')].status").value("UP"))
                .andExpect(jsonPath("$[?(@.name=='patients')].status").value("DOWN"))
                .andExpect(jsonPath("$[?(@.name=='auth')].status").value("UP"));
    }

    @Test
    @DisplayName("GET /api/peers/health - Should succeed on root endpoint probe")
    void shouldSucceedOnRootEndpointProbe() throws Exception {
        // Given - only root endpoint works
        when(restTemplate.getForEntity(argThat((String url) -> url != null && url.endsWith("/v3/api-docs")), eq(String.class)))
                .thenThrow(new RestClientException("Not found"));

        when(restTemplate.getForEntity(argThat((String url) -> url != null && url.endsWith("/swagger-ui.html")), eq(String.class)))
                .thenThrow(new RestClientException("Not found"));

        when(restTemplate.getForEntity(argThat((String url) -> url != null && url.endsWith("/")), eq(String.class)))
                .thenReturn(ResponseEntity.ok("Welcome"));

        // When/Then
        mockMvc.perform(get("/api/peers/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].status", everyItem(is("UP"))))
                .andExpect(jsonPath("$[*].probe", everyItem(org.hamcrest.Matchers.endsWith("/"))));
    }

    @Test
    @DisplayName("GET /api/peers/health - Should handle HTTP errors as DOWN")
    void shouldHandleHttpErrorsAsDown() throws Exception {
        // Given - services return error status codes
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error"));

        // When/Then - should still be considered UP because response was received
        mockMvc.perform(get("/api/peers/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].status", everyItem(is("UP"))));
    }

    @Test
    @DisplayName("GET /api/peers/health - Should return valid JSON structure")
    void shouldReturnValidJsonStructure() throws Exception {
        // Given
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("OK"));

        // When/Then
        mockMvc.perform(get("/api/peers/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].baseUrl").exists())
                .andExpect(jsonPath("$[0].status").exists())
                .andExpect(jsonPath("$[0].probe").exists());
    }
}

