package leti_sisdis_6.happatients.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Security baseline for tests and app:
        // - Disable CSRF to avoid 403 on non-browser clients/tests
        // - Use HTTP Basic to make it easy to authenticate in tests
        // - Require authentication by default; allow "/internal/**" for peer calls if needed
        http
            .csrf(csrf -> csrf.disable())
            .httpBasic(basic -> {})
            .authorizeHttpRequests(auth -> auth
                // allow peer/internal endpoints without auth (adjust if you need to protect them)
                .requestMatchers("/internal/**").permitAll()
                // everything else requires authentication; method-level annotations will enforce roles
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
