package leti_sisdis_6.hapauth.services;

import leti_sisdis_6.hapauth.usermanagement.User;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final RestTemplate restTemplate;
    
    // Hardcoded peer list - simple approach
    private final List<String> peers = Arrays.asList(
        "http://localhost:8085", // instance2
        "http://localhost:8086"  // instance3
    );

    public AuthService(AuthenticationManager authenticationManager, JwtEncoder jwtEncoder, RestTemplate restTemplate) {
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
        this.restTemplate = restTemplate;
    }

    public Authentication authenticate(String username, String password) {
        return authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(username, password)
        );
    }
    
    /**
     * Authenticate with peer forwarding - tries local first, then peers
     */
    public Optional<User> authenticateWithPeers(String username, String password) {
        try {
            // Try local authentication first
            Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
            );
            return Optional.of((User) auth.getPrincipal());
        } catch (Exception e) {
            // If local fails, try peers
            return authenticateInPeers(username, password);
        }
    }

    private Optional<User> authenticateInPeers(String username, String password) {
        for (String peer : peers) {
            try {
                String url = peer + "/api/internal/auth/authenticate";
                AuthRequest request = new AuthRequest(username, password);
                
                User user = restTemplate.postForObject(url, request, User.class);
                if (user != null) {
                    return Optional.of(user);
                }
            } catch (Exception e) {
                // Log and continue to next peer
                System.out.println("Failed to authenticate with peer " + peer + ": " + e.getMessage());
            }
        }
        return Optional.empty();
    }

    public String generateToken(Authentication authentication) {
        Instant now = Instant.now();
        long expiry = 3600L;

        String scope = authentication.getAuthorities().stream()
            .map(authority -> authority.getAuthority())
            .collect(java.util.stream.Collectors.joining(" "));

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("pcmclinic-api")
            .issuedAt(now)
            .expiresAt(now.plusSeconds(expiry))
            .subject(authentication.getName())
            .claim("roles", scope)
            .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        return jwtEncoder.encode(
            JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }
    
    /**
     * Simple authentication request DTO for peer communication
     */
    public static class AuthRequest {
        private String username;
        private String password;
        
        public AuthRequest() {}
        
        public AuthRequest(String username, String password) {
            this.username = username;
            this.password = password;
        }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
} 