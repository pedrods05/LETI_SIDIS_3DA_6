package leti_sisdis_6.hapauth.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuthHelperTest {

    @AfterEach
    void clearCtx() {
        SecurityContextHolder.clearContext();
    }

    private void setAuthorities(String... roles) {
        // As tuas verificações em AuthHelper usam "ADMIN" e "PATIENT" (sem "ROLE_")
        List<SimpleGrantedAuthority> auths = Arrays.stream(roles)
                .map(SimpleGrantedAuthority::new)
                .toList();


        // Authentication REAL com authorities — evita mock do getAuthorities()
        Authentication authentication =
                new UsernamePasswordAuthenticationToken("user", "pw", auths);

        // Apenas o SecurityContext é mockado para devolver o Authentication acima
        SecurityContext context = Mockito.mock(SecurityContext.class);
        Mockito.when(context.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(context);
    }

    @Test
    @DisplayName("isAdmin() → true quando autoridade ADMIN presente")
    void isAdmin_true() {
        setAuthorities("ADMIN", "USER");
        AuthHelper helper = Mockito.mock(AuthHelper.class, Mockito.CALLS_REAL_METHODS);
        assertTrue(helper.isAdmin());
    }

    @Test
    @DisplayName("isAdmin() → false quando ausência de auth ou sem ADMIN")
    void isAdmin_false() {
        // sem autenticação
        SecurityContextHolder.clearContext();
        AuthHelper helper = Mockito.mock(AuthHelper.class, Mockito.CALLS_REAL_METHODS);
        assertFalse(helper.isAdmin());

        // com outra role
        setAuthorities("PATIENT");
        assertFalse(helper.isAdmin());
    }

    @Test
    @DisplayName("isPatient() → true quando autoridade PATIENT presente")
    void isPatient_true() {
        setAuthorities("PATIENT");
        AuthHelper helper = Mockito.mock(AuthHelper.class, Mockito.CALLS_REAL_METHODS);
        assertTrue(helper.isPatient());
    }

    @Test
    @DisplayName("isPatient() → false quando não tem PATIENT")
    void isPatient_false() {
        setAuthorities("ADMIN");
        AuthHelper helper = Mockito.mock(AuthHelper.class, Mockito.CALLS_REAL_METHODS);
        assertFalse(helper.isPatient());
    }
}
