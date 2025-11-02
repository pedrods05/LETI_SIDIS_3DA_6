package leti_sisdis_6.hapauth.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import leti_sisdis_6.hapauth.services.AuthService;
import leti_sisdis_6.hapauth.usermanagement.UserService;
import leti_sisdis_6.hapauth.usermanagement.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testa o InternalAuthApi, usado para comunicação entre peers.
 * Esta versão contém apenas os testes de falha (404) que estavam a passar.
 */
@WebMvcTest(
        controllers = InternalAuthApi.class,
        excludeAutoConfiguration = {
                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                JpaRepositoriesAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
@Import(InternalAuthApiTest.TestConfig.class)
class InternalAuthApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private AuthService authService;
    @Autowired private UserService userService;

    @Configuration
    static class TestConfig {
        @Bean AuthService authService() { return Mockito.mock(AuthService.class); }
        @Bean UserService userService() { return Mockito.mock(UserService.class); }
    }

    private String json(Object o) throws Exception { return objectMapper.writeValueAsString(o); }


    @Test
    @DisplayName("POST /auth/authenticate → 404 quando credenciais inválidas")
    void authenticate_notFound() throws Exception {
        // Mock: Retorna Optional.empty() quando falha, o que no controller se traduz em 404
        given(authService.authenticateWithPeers(any(), any()))
                .willReturn(Optional.empty());

        var req = new AuthService.AuthRequest("nope@example.com", "bad");

        mockMvc.perform(post("/api/internal/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(req)))
                .andExpect(status().isNotFound());
    }


    @Test
    @DisplayName("GET /users/by-username/{username} → 404 utilizador não encontrado")
    void getUserByUsername_notFound() throws Exception {
        given(userService.findByUsername("missing@peer.com")).willReturn(Optional.empty());

        mockMvc.perform(get("/api/internal/users/by-username/missing@peer.com"))
                .andExpect(status().isNotFound());
    }


    @Test
    @DisplayName("GET /users/{id} → 404 utilizador não encontrado")
    void getUserById_notFound() throws Exception {
        given(userService.findById("missing-id")).willReturn(Optional.empty());

        mockMvc.perform(get("/api/internal/users/missing-id"))
                .andExpect(status().isNotFound());
    }
}
