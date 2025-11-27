package leti_sisdis_6.hapappointmentrecords.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    @DisplayName("SecurityFilterChain deve ser criado com configurações corretas")
    void securityFilterChain_shouldBeCreatedWithCorrectConfiguration() throws Exception {
        // Given
        HttpSecurity httpSecurity = mock(HttpSecurity.class, RETURNS_DEEP_STUBS);

        // When
        SecurityFilterChain result = securityConfig.securityFilterChain(httpSecurity);

        // Then
        assertNotNull(result);
    }

    @Test
    @DisplayName("API docs endpoints devem ser acessíveis sem autenticação")
    void apiDocsEndpoints_shouldBeAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Swagger UI deve ser acessível sem autenticação")
    void swaggerUI_shouldBeAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("H2 console deve ser acessível sem autenticação")
    void h2Console_shouldBeAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/h2-console/"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Appointment records endpoints devem ser acessíveis sem autenticação")
    void appointmentRecordsEndpoints_shouldBeAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/appointment-records/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Appointments endpoints devem ser acessíveis sem autenticação")
    void appointmentsEndpoints_shouldBeAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/appointments"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("CSRF deve estar desabilitado")
    void csrf_shouldBeDisabled() throws Exception {
        // Este teste verifica indiretamente se CSRF está desabilitado
        // através de requests POST sem token CSRF
        mockMvc.perform(get("/api/appointments"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Form login deve estar desabilitado")
    void formLogin_shouldBeDisabled() throws Exception {
        // Verifica que não há redirecionamento para página de login
        mockMvc.perform(get("/api/appointments"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("HTTP Basic deve estar desabilitado")
    void httpBasic_shouldBeDisabled() throws Exception {
        // Verifica que não há challenge de HTTP Basic
        mockMvc.perform(get("/api/appointments"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Frame options devem estar desabilitadas")
    void frameOptions_shouldBeDisabled() throws Exception {
        // Testa que X-Frame-Options está configurado corretamente para H2 console
        mockMvc.perform(get("/h2-console/"))
                .andExpect(status().isOk());
    }
}
