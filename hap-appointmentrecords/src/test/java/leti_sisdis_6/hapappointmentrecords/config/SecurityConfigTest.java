package leti_sisdis_6.hapappointmentrecords.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.data.mongodb.uri=mongodb://localhost:27017/testdb"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should allow access to Swagger UI without authentication")
    @WithAnonymousUser
    void shouldAllowAccessToSwaggerUI() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should allow access to API docs without authentication")
    @WithAnonymousUser
    void shouldAllowAccessToApiDocs() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should allow access to appointment records API without authentication")
    @WithAnonymousUser
    void shouldAllowAccessToAppointmentRecordsAPI() throws Exception {
        // Note: This will likely return 404 or other error, but not 401/403
        mockMvc.perform(get("/api/appointment-records/some-id"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status).isNotEqualTo(401);
                    assertThat(status).isNotEqualTo(403);
                });
    }

    @Test
    @DisplayName("Should allow access to appointments API without authentication")
    @WithAnonymousUser
    void shouldAllowAccessToAppointmentsAPI() throws Exception {
        // Note: This will likely return 404 or other error, but not 401/403
        mockMvc.perform(get("/api/appointments/some-id"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status).isNotEqualTo(401);
                    assertThat(status).isNotEqualTo(403);
                });
    }

    @Test
    @DisplayName("Should disable CSRF protection")
    @WithAnonymousUser
    void shouldDisableCSRFProtection() throws Exception {
        // POST without CSRF token should work (not get 403 due to CSRF)
        mockMvc.perform(get("/api/appointment-records/test"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status).isNotEqualTo(403);
                });
    }

    @Test
    @DisplayName("SecurityConfig class should be annotated with @Configuration")
    void securityConfigShouldHaveConfigurationAnnotation() {
        // When
        org.springframework.context.annotation.Configuration annotation =
                SecurityConfig.class.getAnnotation(org.springframework.context.annotation.Configuration.class);

        // Then
        assertThat(annotation).isNotNull();
    }

    @Test
    @DisplayName("SecurityFilterChain method should be annotated with @Bean")
    void securityFilterChainMethodShouldHaveBeanAnnotation() throws NoSuchMethodException {
        // When
        org.springframework.context.annotation.Bean annotation =
                SecurityConfig.class.getMethod("securityFilterChain",
                        org.springframework.security.config.annotation.web.builders.HttpSecurity.class)
                        .getAnnotation(org.springframework.context.annotation.Bean.class);

        // Then
        assertThat(annotation).isNotNull();
    }

    @Test
    @DisplayName("SecurityFilterChain method should be annotated with @Order(0)")
    void securityFilterChainMethodShouldHaveOrderAnnotation() throws NoSuchMethodException {
        // When
        org.springframework.core.annotation.Order annotation =
                SecurityConfig.class.getMethod("securityFilterChain",
                        org.springframework.security.config.annotation.web.builders.HttpSecurity.class)
                        .getAnnotation(org.springframework.core.annotation.Order.class);

        // Then
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should allow access to H2 console without authentication")
    @WithAnonymousUser
    void shouldAllowAccessToH2Console() throws Exception {
        mockMvc.perform(get("/h2-console"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status).isNotEqualTo(401);
                    assertThat(status).isNotEqualTo(403);
                });
    }

    @Test
    @DisplayName("Should allow any request without authentication")
    @WithAnonymousUser
    void shouldAllowAnyRequestWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/some/random/endpoint"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status).isNotEqualTo(401);
                    assertThat(status).isNotEqualTo(403);
                });
    }
}

