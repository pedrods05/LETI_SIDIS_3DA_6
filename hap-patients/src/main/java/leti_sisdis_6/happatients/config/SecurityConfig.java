package leti_sisdis_6.happatients.config;

import jakarta.annotation.PostConstruct;
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
    @org.springframework.beans.factory.annotation.Value("${jwt.secret.key}")
    private String jwtSecretKey;
    @PostConstruct
    public void logSecretKey() {
        System.out.println("==============================================");
        System.out.println(">>> JWT SECRET LOADED: " + jwtSecretKey);
        System.out.println("==============================================");
    }
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

            // --- CORREÇÃO: Lidar com 'authorities' (Lista ou String) ---
            Object authorities = jwt.getClaim("authorities");
            if (authorities instanceof List<?> list) {
                merged.addAll(list.stream().filter(Objects::nonNull).map(Object::toString).map(String::trim)
                        .filter(s -> !s.isEmpty()).map(SimpleGrantedAuthority::new).collect(Collectors.toSet()));
            } else if (authorities instanceof String strAuth) {
                // Se vier como string única
                Arrays.stream(strAuth.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                        .map(SimpleGrantedAuthority::new).forEach(merged::add);
            }

            // --- CORREÇÃO CRÍTICA: Lidar com 'roles' (Lista ou String) ---
            Object rolesClaim = jwt.getClaim("roles");
            if (rolesClaim instanceof List<?> list) {
                merged.addAll(list.stream().filter(Objects::nonNull).map(Object::toString).map(String::trim)
                        .filter(s -> !s.isEmpty()).map(SimpleGrantedAuthority::new).collect(Collectors.toSet()));
            } else if (rolesClaim instanceof String strRole) {
                // *** AQUI ESTÁ A CURA PARA O TEU PROBLEMA ***
                // Se for "ADMIN", adiciona a authority "ADMIN"
                Arrays.stream(strRole.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                        .map(SimpleGrantedAuthority::new).forEach(merged::add);
            }

            // --- Realm Access (Keycloak style) ---
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

    @Bean
    public JwtDecoder jwtDecoder() {
        byte[] secret = jwtSecretKey.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        SecretKeySpec key = new SecretKeySpec(secret, "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256)
                .build();    }

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
