package leti_sisdis_6.hapauth.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import leti_sisdis_6.hapauth.services.AuthService;
import leti_sisdis_6.hapauth.usermanagement.UserService;
import leti_sisdis_6.hapauth.usermanagement.model.User;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = InternalAuthApi.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(InternalAuthApiTest.TestConfig.class)
class InternalAuthApiTest {

    @TestConfiguration
    static class TestConfig {
        @Bean AuthService authService() { return Mockito.mock(AuthService.class); }
        @Bean UserService userService() { return Mockito.mock(UserService.class); }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Test
    @DisplayName("GET /api/internal/users/{id} → 200 ou 404")
    void getUser() throws Exception {
        User user = new User();
        user.setId("u1");
        user.setUsername("in@example.com");

        given(userService.findById("u1")).willReturn(Optional.of(user));
        given(userService.findById("missing")).willReturn(Optional.empty());

        mockMvc.perform(get("/api/internal/users/u1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("u1")));

        mockMvc.perform(get("/api/internal/users/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/internal/auth/authenticate → 200 com utilizador ou 404")
    void authenticate() throws Exception {
        User user = new User();
        user.setId("u2");
        user.setUsername("peer@example.com");

        given(authService.authenticateWithPeers(eq("peer@example.com"), eq("pw")))
                .willReturn(Optional.of(user));
        given(authService.authenticateWithPeers(eq("nope@example.com"), eq("pw")))
                .willReturn(Optional.empty());

        var ok = new AuthService.AuthRequest("peer@example.com", "pw");
        var ko = new AuthService.AuthRequest("nope@example.com", "pw");

        mockMvc.perform(post("/api/internal/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ok)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("u2")));

        mockMvc.perform(post("/api/internal/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ko)))
                .andExpect(status().isNotFound());
    }
}
