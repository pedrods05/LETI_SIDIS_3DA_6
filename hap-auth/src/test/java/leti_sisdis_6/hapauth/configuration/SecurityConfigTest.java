package leti_sisdis_6.hapauth.configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment = WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration"
        }
)
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtDecoder jwtDecoder;

    // <<-- usa o nome do bean definido na tua SecurityConfig
    @Autowired @Qualifier("publicFilterChain")
    SecurityFilterChain publicFilterChain;

    @Test
    @DisplayName("PasswordEncoder deve ser BCrypt e validar o hash")
    void passwordEncoder_shouldBeBcrypt() {
        assertNotNull(passwordEncoder);
        String raw = "secret";
        String hash = passwordEncoder.encode(raw);
        assertTrue(passwordEncoder.matches(raw, hash));
    }

    @Test
    @DisplayName("JwtDecoder deve existir")
    void jwtDecoder_shouldExist() {
        assertNotNull(jwtDecoder);
    }

    @Test
    @DisplayName("SecurityFilterChain (publicFilterChain) deve existir")
    void securityFilterChain_shouldExist() {
        assertNotNull(publicFilterChain);
    }
}
