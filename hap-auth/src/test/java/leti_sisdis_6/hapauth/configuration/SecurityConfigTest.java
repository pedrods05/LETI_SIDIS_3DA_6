package leti_sisdis_6.hapauth.configuration;

import leti_sisdis_6.hapauth.usermanagement.repository.UserInMemoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest
@Import({SecurityConfig.class, SecurityConfigTest.TestConfig.class, SecurityConfigTest.DummyController.class})
@AutoConfigureMockMvc // Liga os filtros de segurança (NÃO usar addFilters = false)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder; // Injetado a partir do SecurityConfig real


    @Configuration
    static class TestConfig {
        @Bean
        UserInMemoryRepository userInMemoryRepository() {
            return Mockito.mock(UserInMemoryRepository.class);
        }
    }


    @RestController
    static class DummyController {
        @GetMapping("/api/public/test") public String publicRoute() { return "public"; }
        @GetMapping("/api/internal/test") public String internalRoute() { return "internal"; }
        @GetMapping("/h2-console/test") public String h2Route() { return "h2"; }
        @GetMapping("/swagger-ui/test") public String swaggerRoute() { return "swagger"; }
        @GetMapping("/api/v1/protected/test") public String protectedRoute() { return "protected"; }
    }

    @Test
    @DisplayName("Rotas públicas devem ser permitidas (permitAll)")
    void publicRoutes_arePermitted() throws Exception {
        mockMvc.perform(get("/api/public/test"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/internal/test"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/h2-console/test"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/swagger-ui/test"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Rota protegida deve falhar (401 Unauthorized) sem token")
    void protectedRoute_isUnauthorized_withoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/protected/test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Rota protegida deve funcionar (200 OK) com token JWT válido")
    void protectedRoute_isOk_withValidToken() throws Exception {
        String token = generateTestToken("user", "ROLE_USER");

        mockMvc.perform(get("/api/v1/protected/test")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }


    private String generateTestToken(String subject, String... roles) {
        Instant now = Instant.now();

        // CORREÇÃO: Especificar o algoritmo HS256 (MAC) no header
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("self")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .subject(subject)
                .claim("roles", List.of(roles))
                .build();

        return this.jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
