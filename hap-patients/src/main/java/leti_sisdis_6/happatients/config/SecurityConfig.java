package leti_sisdis_6.happatients.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.util.*;
import java.util.stream.Collectors;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .httpBasic(basic -> {})
            .oauth2ResourceServer(oauth -> oauth
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/internal/**").permitAll()
                .requestMatchers("/api/v2/patients/register").permitAll()
                // Require either ADMIN authority from JWT or ROLE_ADMIN from Basic users
                .requestMatchers("/patients/**").hasAnyAuthority("ADMIN", "ROLE_ADMIN")
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopesConverter = new JwtGrantedAuthoritiesConverter();
        scopesConverter.setAuthorityPrefix("");
        scopesConverter.setAuthoritiesClaimName("scope");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Set<GrantedAuthority> merged = new HashSet<>();
            Collection<GrantedAuthority> scopeAuth = scopesConverter.convert(jwt);
            if (scopeAuth != null) merged.addAll(scopeAuth);

            Object authorities = jwt.getClaim("authorities");
            if (authorities instanceof List<?> list) {
                merged.addAll(list.stream().filter(Objects::nonNull).map(Object::toString).map(String::trim)
                    .filter(s -> !s.isEmpty()).map(SimpleGrantedAuthority::new).collect(Collectors.toSet()));
            }
            Object rolesClaim = jwt.getClaim("roles");
            if (rolesClaim instanceof List<?> list) {
                merged.addAll(list.stream().filter(Objects::nonNull).map(Object::toString).map(String::trim)
                    .filter(s -> !s.isEmpty()).map(SimpleGrantedAuthority::new).collect(Collectors.toSet()));
            }
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess != null) {
                Object realmRoles = realmAccess.get("roles");
                if (realmRoles instanceof List<?> list) {
                    merged.addAll(list.stream().filter(Objects::nonNull).map(Object::toString).map(String::trim)
                        .filter(s -> !s.isEmpty()).map(SimpleGrantedAuthority::new).collect(Collectors.toSet()));
                }
            }
            return merged;
        });
        return converter;
    }

    // Provide a local HS256 JwtDecoder so tests and basic auth can coexist without external issuer
    @Bean
    public JwtDecoder jwtDecoder() {
        // NOTE: This secret is for dev/test; if you use a real issuer, replace with JwtDecoders.fromIssuerLocation(...)
        byte[] secret = "dev-signing-key-please-change".getBytes();
        SecretKeySpec key = new SecretKeySpec(secret, "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    @Bean
    public UserDetailsService userDetailsService(org.springframework.security.crypto.password.PasswordEncoder encoder) {
        UserDetails admin = User.withUsername("admin")
                .password(encoder.encode("admin"))
                .roles("ADMIN")
                .build();
        UserDetails physician = User.withUsername("physician")
                .password(encoder.encode("physician"))
                .roles("PHYSICIAN")
                .build();
        UserDetails patient = User.withUsername("patient")
                .password(encoder.encode("patient"))
                .roles("PATIENT")
                .build();
        return new InMemoryUserDetailsManager(admin, physician, patient);
    }
}
