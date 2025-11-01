package leti_sisdis_6.hapauth.configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários focados no comportamento do bean, sem subir o contexto completo.
 * Aqui mockamos o HttpSecurity e apenas verificamos a interação/configuração básica.
 */
class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    @DisplayName("PasswordEncoder deve ser BCrypt e validar password corretamente")
    void passwordEncoder_shouldBeBcrypt() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        assertNotNull(encoder);
        String raw = "secret";
        String hash = encoder.encode(raw);
        assertTrue(encoder.matches(raw, hash), "BCrypt deve validar o hash gerado");
    }

    @Test
    @DisplayName("JwtDecoder deve ser criado (chave secreta configurada)")
    void jwtDecoder_shouldBeCreated() {
        JwtDecoder decoder = securityConfig.jwtDecoder();
        assertNotNull(decoder, "JwtDecoder não deve ser nulo");
    }

    @Test
    @DisplayName("JwtAuthenticationConverter deve ser criado")
    void jwtAuthenticationConverter_shouldBeCreated() {
        assertNotNull(securityConfig.jwtAuthenticationConverter(),
                "Converter de autenticação JWT não deve ser nulo");
    }

    @Test
    @DisplayName("SecurityFilterChain deve ser construído com HttpSecurity e AuthenticationProvider")
    void securityFilterChain_shouldBuild() throws Exception {
        // Mocks do HttpSecurity e método encadeados
        HttpSecurity http = mock(HttpSecurity.class, RETURNS_DEEP_STUBS);
        AuthenticationProvider authenticationProvider = mock(AuthenticationProvider.class);
        SecurityFilterChain mockChain = mock(SecurityFilterChain.class);

        // Encadeamentos comuns: http.csrf(...), http.headers(...), http.sessionManagement(...),
        // http.authorizeHttpRequests(...), http.authenticationProvider(...), http.oauth2ResourceServer(...)
        when(http.csrf(any())).thenReturn(http);
        when(http.headers(any())).thenReturn(http);
        when(http.sessionManagement(any())).thenReturn(http);
        when(http.authorizeHttpRequests(any())).thenReturn(http);
        when(http.authenticationProvider(any())).thenReturn(http);
        when(http.oauth2ResourceServer(any())).thenReturn(http);
        when(http.build()).thenReturn(mockChain);

        SecurityFilterChain chain = securityConfig.securityFilterChain(http, authenticationProvider);
        assertNotNull(chain, "SecurityFilterChain não deve ser nulo");

        // Verificar que os passos principais foram invocados
        verify(http, atLeastOnce()).csrf(any());
        verify(http, atLeastOnce()).headers(any());
        verify(http, atLeastOnce()).sessionManagement(any());
        verify(http, atLeastOnce()).authorizeHttpRequests(any());
        verify(http, atLeastOnce()).authenticationProvider(authenticationProvider);
        verify(http, atLeastOnce()).oauth2ResourceServer(any());
        verify(http, times(1)).build();
    }
}
