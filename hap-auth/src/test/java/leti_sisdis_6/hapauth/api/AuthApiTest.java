package leti_sisdis_6.hapauth.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import leti_sisdis_6.hapauth.dto.LoginRequest;
import leti_sisdis_6.hapauth.dto.RegisterUserRequest;
import leti_sisdis_6.hapauth.dto.UserIdResponse;
import leti_sisdis_6.hapauth.services.AuthService;
import leti_sisdis_6.hapauth.usermanagement.UserService;
import leti_sisdis_6.hapauth.usermanagement.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(
        controllers = AuthApi.class,
        excludeAutoConfiguration = {
                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                JpaRepositoriesAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestConfig.class)
class AuthApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private AuthService authService;
    @MockitoBean private UserService userService;
    @MockitoBean private RestTemplate restTemplate;

    @MockitoBean(name = "userRepository")
    private Object userRepository;

    private String json(Object o) throws Exception { return objectMapper.writeValueAsString(o); }

    private LoginRequest loginReq(String u, String p) {
        var r = new LoginRequest();
        r.setUsername(u);
        r.setPassword(p);
        return r;
    }

    @Test
    @DisplayName("POST /api/public/login → 200 local")
    void login_local_ok() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken(
                "john@example.com", "secret",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        given(authService.authenticate(eq("john@example.com"), eq("secret"))).willReturn(auth);
        given(authService.generateToken(eq(auth))).willReturn("token-123");

        mockMvc.perform(post("/api/public/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("utf-8")
                        .content(json(loginReq("john@example.com", "secret"))))
                .andExpect(status().isOk())
                .andExpect(header().string("Authorization", containsString("Bearer token-123")))
                .andExpect(jsonPath("$.token", is("token-123")))
                .andExpect(jsonPath("$.roles[0]", is("ROLE_USER")));
    }

    @Test
    @DisplayName("POST /api/public/login → 401 fail-fast (invalid local creds)")
    void login_invalid_fail_fast() throws Exception {
        given(authService.authenticate(eq("peer@example.com"), eq("pw")))
                .willThrow(new RuntimeException("bad creds"));

        mockMvc.perform(post("/api/public/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("utf-8")
                        .content(json(loginReq("peer@example.com", "pw"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Invalid username or password")));
    }

    @Test
    @DisplayName("POST /api/public/login → 401 quando tudo falha")
    void login_all_fail() throws Exception {
        given(authService.authenticate(eq("nope@example.com"), eq("pw")))
                .willThrow(new RuntimeException("bad"));
        given(restTemplate.postForObject(anyString(),
                any(AuthService.AuthRequest.class), eq(User.class)))
                .willReturn(null);

        mockMvc.perform(post("/api/public/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("utf-8")
                        .content(json(loginReq("nope@example.com", "pw"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Invalid username or password")));
    }

    @Test
    @DisplayName("POST /api/public/register → 201")
    void register_created() throws Exception {
        var req = new RegisterUserRequest();
        req.setUsername("new@example.com");
        req.setPassword("pw");
        req.setRole("PATIENT");

        var resp = UserIdResponse.builder()
                .id("abc-123").username("new@example.com").role("PATIENT").build();

        given(userService.register(any(RegisterUserRequest.class))).willReturn(resp);

        mockMvc.perform(post("/api/public/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("utf-8")
                        .content(json(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is("abc-123")))
                .andExpect(jsonPath("$.username", is("new@example.com")))
                .andExpect(jsonPath("$.role", is("PATIENT")));
    }


}