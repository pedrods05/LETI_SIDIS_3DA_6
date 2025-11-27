package leti_sisdis_6.hapauth.services;

import leti_sisdis_6.hapauth.usermanagement.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final JwtEncoder jwtEncoder = mock(JwtEncoder.class);
    private final RestTemplate restTemplate = mock(RestTemplate.class);

    private final AuthService authService = new AuthService(authenticationManager, jwtEncoder, restTemplate);

    @Test
    @DisplayName("authenticate() → deve delegar no AuthenticationManager e devolver o Authentication resultante")
    void authenticate_shouldDelegateToManager() {
        Authentication mockAuth = mock(Authentication.class);
        given(authenticationManager.authenticate(any(Authentication.class))).willReturn(mockAuth);

        Authentication result = authService.authenticate("user", "pw");

        assertNotNull(result);
        verify(authenticationManager).authenticate(any(Authentication.class));
    }

    @Test
    @DisplayName("authenticateWithPeers() → devolve user local se autenticação local for bem sucedida")
    void authenticateWithPeers_localSuccess() {
        User user = new User();
        Authentication auth = new UsernamePasswordAuthenticationToken(user, "pw");
        given(authenticationManager.authenticate(any(Authentication.class))).willReturn(auth);

        Optional<User> result = authService.authenticateWithPeers("local", "pw");

        assertTrue(result.isPresent());
        assertEquals(user, result.get());
    }

    @Test
    @DisplayName("authenticateWithPeers() → tenta peers quando local falha e devolve user remoto se encontrado")
    void authenticateWithPeers_peerSuccess() {
        // local falha
        given(authenticationManager.authenticate(any(Authentication.class)))
                .willThrow(new RuntimeException("bad credentials"));

        // peer devolve um user
        User remoteUser = new User();
        remoteUser.setId("u1");
        remoteUser.setUsername("peer@example.com");

        given(restTemplate.postForObject(anyString(), any(AuthService.AuthRequest.class), eq(User.class)))
                .willReturn(remoteUser);

        Optional<User> result = authService.authenticateWithPeers("peer@example.com", "pw");

        assertTrue(result.isPresent());
        assertEquals("peer@example.com", result.get().getUsername());
    }

    @Test
    @DisplayName("authenticateWithPeers() → devolve empty quando local e peers falham")
    void authenticateWithPeers_allFail() {
        given(authenticationManager.authenticate(any(Authentication.class)))
                .willThrow(new RuntimeException("bad creds"));
        given(restTemplate.postForObject(anyString(), any(AuthService.AuthRequest.class), eq(User.class)))
                .willThrow(new RuntimeException("peer down"));

        Optional<User> result = authService.authenticateWithPeers("none", "pw");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("generateToken() → deve construir claims e delegar no JwtEncoder")
    void generateToken_shouldEncodeJwt() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user",
                "pw",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        var encoderResult = mock(org.springframework.security.oauth2.jwt.JwtEncoderParameters.class);
        var jwtMock = mock(org.springframework.security.oauth2.jwt.Jwt.class);
        when(jwtMock.getTokenValue()).thenReturn("jwt-123");

        ArgumentCaptor<JwtEncoderParameters> captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        when(jwtEncoder.encode(captor.capture())).thenReturn(jwtMock);

        String token = authService.generateToken(auth);

        assertEquals("jwt-123", token);
        JwtEncoderParameters params = captor.getValue();
        assertNotNull(params);
        assertEquals(MacAlgorithm.HS256, params.getJwsHeader().getAlgorithm());
    }

    @Test
    @DisplayName("generateTokenForUser() → deve gerar token com roles do utilizador")
    void generateTokenForUser_shouldIncludeRoles() {
        User user = new User() {
            @Override
            public Collection<SimpleGrantedAuthority> getAuthorities() {
                return List.of(new SimpleGrantedAuthority("ROLE_USER"));
            }
        };
        user.setUsername("john@example.com");

        var jwtMock = mock(org.springframework.security.oauth2.jwt.Jwt.class);
        when(jwtMock.getTokenValue()).thenReturn("user-token");
        when(jwtEncoder.encode(any())).thenReturn(jwtMock);

        String token = authService.generateTokenForUser(user);

        assertEquals("user-token", token);
        verify(jwtEncoder).encode(any());
    }

    @Test
    @DisplayName("AuthRequest → getters e setters funcionam corretamente")
    void authRequest_gettersSetters() {
        AuthService.AuthRequest req = new AuthService.AuthRequest("u", "p");
        assertEquals("u", req.getUsername());
        assertEquals("p", req.getPassword());

        req.setUsername("x");
        req.setPassword("y");
        assertEquals("x", req.getUsername());
        assertEquals("y", req.getPassword());
    }
}
