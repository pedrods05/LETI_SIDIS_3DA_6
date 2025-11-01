package leti_sisdis_6.hapauth.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import leti_sisdis_6.hapauth.dto.LoginRequest;
import leti_sisdis_6.hapauth.dto.LoginResponse;
import leti_sisdis_6.hapauth.dto.RegisterUserRequest;
import leti_sisdis_6.hapauth.dto.UserIdResponse;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthApi.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(AuthApiTest.TestConfig.class)
class AuthApiTest {

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
    @DisplayName("POST /api/auth/login → 200 com token e role")
    void login_ok() throws Exception {
        given(authService.login(any(LoginRequest.class))).willReturn(new LoginResponse("token-123", "USER"));

        var req = new LoginRequest();
        req.setUsername("john@example.com");
        req.setPassword("secret");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is("token-123")))
                .andExpect(jsonPath("$.role", is("USER")));
    }

    @Test
    @DisplayName("POST /api/users/register → 201 com id do utilizador")
    void register_ok() throws Exception {
        given(authService.register(any(RegisterUserRequest.class))).willReturn(new UserIdResponse("abc-123"));

        var req = new RegisterUserRequest();
        req.setUsername("new@example.com");
        req.setPassword("pw");
        req.setRole("PATIENT");

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId", is("abc-123")));
    }

    @Test
    @DisplayName("GET /api/users/{id} → 200 quando existe, 404 quando não")
    void getUser_variants() throws Exception {
        User user = new User();
        user.setId("u1");
        user.setUsername("john@example.com");
        given(userService.findById("u1")).willReturn(Optional.of(user));
        given(userService.findById("missing")).willReturn(Optional.empty());

        mockMvc.perform(get("/api/users/u1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("u1")));

        mockMvc.perform(get("/api/users/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/auth/authenticate → 200 com utilizador ou 404")
    void authenticate_withPeers() throws Exception {
        User okUser = new User();
        okUser.setId("u2");
        okUser.setUsername("peer@example.com");

        given(authService.authenticateWithPeers(eq("peer@example.com"), eq("pw")))
                .willReturn(Optional.of(okUser));
        given(authService.authenticateWithPeers(eq("nope@example.com"), eq("pw")))
                .willReturn(Optional.empty());

        var ok = new AuthService.AuthRequest("peer@example.com", "pw");
        var ko = new AuthService.AuthRequest("nope@example.com", "pw");

        mockMvc.perform(post("/api/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ok)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("u2")));

        mockMvc.perform(post("/api/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ko)))
                .andExpect(status().isNotFound());
    }
}
