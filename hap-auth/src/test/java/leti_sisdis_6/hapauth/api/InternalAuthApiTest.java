package leti_sisdis_6.hapauth.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import leti_sisdis_6.hapauth.services.AuthService;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Path esperado:
 *  - POST /api/internal/auth/authenticate
 * Confirma o @RequestMapping do teu controller interno e ajusta se necessário.
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
    @Autowired private AuthService authService; // mock (bean)

    @Configuration
    static class TestConfig {
        @Bean AuthService authService() { return Mockito.mock(AuthService.class); }
    }

    private String json(Object o) throws Exception { return objectMapper.writeValueAsString(o); }

    @Test
    @DisplayName("POST /api/internal/auth/authenticate → 200")
    void authenticate_ok() throws Exception {
        User principal = new User();
        principal.setId("u1");
        principal.setUsername("john@example.com");

        var authentication = new UsernamePasswordAuthenticationToken(
                principal, "pw", List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        given(authService.authenticate(eq("john@example.com"), eq("pw"))).willReturn(authentication);

        var req = new AuthService.AuthRequest("john@example.com", "pw");

        mockMvc.perform(post("/api/internal/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("utf-8")
                        .content(json(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("u1")))
                .andExpect(jsonPath("$.username", is("john@example.com")));
    }

    @Test
    @DisplayName("POST /api/internal/auth/authenticate → 404 em credenciais inválidas")
    void authenticate_notFound() throws Exception {
        given(authService.authenticate(any(), any())).willThrow(new RuntimeException("bad creds"));

        var req = new AuthService.AuthRequest("nope@example.com", "bad");

        mockMvc.perform(post("/api/internal/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("utf-8")
                        .content(json(req)))
                .andExpect(status().isNotFound());
    }
}
