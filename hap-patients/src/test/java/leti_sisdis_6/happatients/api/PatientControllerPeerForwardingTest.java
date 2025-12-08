package leti_sisdis_6.happatients.api;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import leti_sisdis_6.happatients.dto.PatientDetailsDTO;
import leti_sisdis_6.happatients.http.ResilientRestTemplate;
import leti_sisdis_6.happatients.repository.PatientLocalRepository;
import leti_sisdis_6.happatients.service.PatientService;
import leti_sisdis_6.happatients.service.PatientQueryService;
import leti_sisdis_6.happatients.exceptions.NotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import java.util.Optional;

@WithMockUser(authorities = "ADMIN")
@AutoConfigureMockMvc
@WebMvcTest(
        controllers = PatientController.class,
        properties = {
                "hap.patients.peers=http://localhost:18080",
                "server.port=0"
        }
)
@Import({PatientControllerPeerForwardingTest.TestSecurityConfig.class, PatientController.class})
class PatientControllerPeerForwardingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PatientService patientService;

    @MockBean
    private PatientLocalRepository localRepository;

    @MockBean
    private ResilientRestTemplate resilientRestTemplate;

    @MockBean
    private PatientQueryService patientQueryService;

    // Minimal security configuration for the test slice
    @Configuration
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            // Disable CSRF and allow all for controller testing; we still attach a user via .with(user(...))
            http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @BeforeEach
    void setup() {
        // Default CQRS query service to simulate not found so controller will consult service/peers
        when(patientQueryService.getPatientProfile(anyString())).thenThrow(new NotFoundException("not found"));
    }

    @Test
    void whenNotFoundLocally_shouldForwardToPeer_andReturnPeerResult() throws Exception {
        String id = "PAT01";
        when(localRepository.findById(id)).thenReturn(Optional.empty());
        when(patientService.getPatientDetails(id)).thenThrow(new EntityNotFoundException("not found"));
        PatientDetailsDTO remote = PatientDetailsDTO.builder()
                .patientId(id)
                .fullName("Alice Peer")
                .email("alice@peer.test")
                .build();
        when(resilientRestTemplate.getForObjectWithFallback(anyString(), eq(PatientDetailsDTO.class)))
                .thenReturn(remote);

        mockMvc.perform(get("/patients/{id}", id)
                        .accept("application/json")
                        .with(user("admin").authorities(new SimpleGrantedAuthority("ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(id))
                .andExpect(jsonPath("$.fullName").value("Alice Peer"))
                .andExpect(jsonPath("$.email").value("alice@peer.test"));

        verify(resilientRestTemplate, atLeastOnce()).getForObjectWithFallback(contains("/internal/patients/" + id), eq(PatientDetailsDTO.class));
    }

    @Test
    void whenNotFoundLocally_andPeerFails_shouldReturn404() throws Exception {
        String id = "PAT404";
        when(localRepository.findById(id)).thenReturn(Optional.empty());
        when(patientService.getPatientDetails(id)).thenThrow(new EntityNotFoundException("not found"));
        when(resilientRestTemplate.getForObjectWithFallback(anyString(), eq(PatientDetailsDTO.class)))
                .thenThrow(new RuntimeException("peer down"));

        mockMvc.perform(get("/patients/{id}", id)
                        .accept("application/json")
                        .with(user("admin").authorities(new SimpleGrantedAuthority("ADMIN"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void whenLocalHasData_shouldReturnLocal_andNeverCallPeers() throws Exception {
        String id = "PAT_LOCAL";
        PatientDetailsDTO local = PatientDetailsDTO.builder()
                .patientId(id)
                .fullName("Local User")
                .email("local@example.com")
                .build();
        // Controller reads from patientService after CQRS fallback
        when(patientService.getPatientDetails(id)).thenReturn(local);

        mockMvc.perform(get("/patients/{id}", id)
                        .accept("application/json")
                        .with(user("admin").authorities(new SimpleGrantedAuthority("ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(id))
                .andExpect(jsonPath("$.fullName").value("Local User"))
                .andExpect(jsonPath("$.email").value("local@example.com"));

        verifyNoInteractions(resilientRestTemplate);
    }

    @Test
    void whenIdInvalid_orNotFoundAnywhere_shouldReturn404() throws Exception {
        String id = "INVALID_404";
        when(localRepository.findById(id)).thenReturn(Optional.empty());
        when(patientService.getPatientDetails(id)).thenThrow(new EntityNotFoundException("not found"));
        when(resilientRestTemplate.getForObjectWithFallback(anyString(), eq(PatientDetailsDTO.class)))
                .thenReturn(null);

        mockMvc.perform(get("/patients/{id}", id)
                        .accept("application/json")
                        .with(user("admin").authorities(new SimpleGrantedAuthority("ADMIN"))))
                .andExpect(status().isNotFound());
    }

}
