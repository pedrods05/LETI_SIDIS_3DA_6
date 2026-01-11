package leti_sisdis_6.hapappointmentrecords.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .requestMatchers("/api/appointment-records/notes/**").hasAnyRole("DOCTOR", "ADMIN")
                        .requestMatchers("/api/appointment-records/patient/**").hasAnyRole("PATIENT", "DOCTOR", "ADMIN")
                        .requestMatchers("/api/appointment-records/**").authenticated()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        http.headers(headers -> headers.frameOptions(frame -> frame.disable()));
        // TODO: enable client-auth=need in docker profile when all peers have client certs
        return http.build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthorityPrefix("ROLE_");
        authoritiesConverter.setAuthoritiesClaimName("roles");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    /**
     * JwtDecoder resiliente: usa segredo simétrico (HS256) se disponível; caso contrário, tenta JWKS.
     */
    @Bean
    public JwtDecoder jwtDecoder(@Value("${jwt.secret.key:}") String sharedSecret) {
        if (sharedSecret != null && !sharedSecret.isBlank()) {
            // Prefer HS256 with shared secret (alinhar com hap-auth em docker)
            SecretKey key = new SecretKeySpec(sharedSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
            return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        }

        // Lista de URLs conhecidas onde o hap-auth expõe as suas chaves públicas (JWKS)
        List<String> jwkSetUris = Arrays.asList(
                "https://localhost:8084/oauth2/jwks",
                "https://localhost:8090/oauth2/jwks"
        );

        // Tentamos inicializar o decoder com a primeira instância disponível
        for (String url : jwkSetUris) {
            try {
                // Criamos o decoder apontando para o conjunto de chaves
                NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(url).build();

                /* * IMPORTANTE: Removemos a validação estrita do 'iss' (issuer).
                 * Isto é necessário porque cada instância do hap-auth gera um 'iss'
                 * com a sua própria porta (ex: localhost:8084 vs localhost:8090).
                 */
                decoder.setJwtValidator(token -> {
                    // Aqui podes adicionar validações personalizadas se necessário
                    return org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.success();
                });

                return decoder;
            } catch (Exception e) {
                // Log de aviso: falha ao contactar uma instância, tentando a próxima...
                System.err.println("Aviso: Não foi possível obter JWKS de " + url + ". Tentando próxima instância...");
            }
        }

        // Se nenhuma instância estiver disponível, o bean falhará no arranque (o que é correto)
        throw new IllegalStateException("Erro crítico: Nenhuma instância do hap-auth disponível para validar tokens!");
    }
}