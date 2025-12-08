package leti_sisdis_6.happatients.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Provides a PasswordEncoder bean for services that require password hashing.
 * Keep it isolated to avoid interfering with other security configuration.
 */
@Configuration
public class SecurityBeansConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt is the recommended encoder for Spring Security
        return new BCryptPasswordEncoder();
    }
}

