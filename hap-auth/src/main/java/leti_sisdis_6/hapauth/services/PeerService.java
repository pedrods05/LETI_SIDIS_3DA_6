package leti_sisdis_6.hapauth.services;

import leti_sisdis_6.hapauth.configuration.PeerConfig;
import leti_sisdis_6.hapauth.usermanagement.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PeerService {
    
    private final RestTemplate restTemplate;
    private final PeerConfig peerConfig;

    public Optional<User> findUserInPeers(String username) {
        if (!peerConfig.isEnabled()) {
            return Optional.empty();
        }
        
        List<String> activePeers = peerConfig.getActivePeers();
        log.debug("Searching for user '{}' in {} peers", username, activePeers.size());
        
        for (String peerUrl : activePeers) {
            try {
                String url = peerUrl + "/api/internal/users/by-username/" + username;
                log.debug("Querying peer: {}", url);
                
                ResponseEntity<User> response = restTemplate.getForEntity(url, User.class);
                
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    log.info("Found user '{}' in peer: {}", username, peerUrl);
                    return Optional.of(response.getBody());
                }
                
            } catch (HttpClientErrorException.NotFound e) {
                log.debug("User '{}' not found in peer: {}", username, peerUrl);
                // Continue to next peer
            } catch (ResourceAccessException e) {
                log.warn("Failed to connect to peer {}: {}", peerUrl, e.getMessage());
                // Continue to next peer
            } catch (Exception e) {
                log.warn("Error querying peer {} for user '{}': {}", peerUrl, username, e.getMessage());
                // Continue to next peer
            }
        }
        
        log.debug("User '{}' not found in any peer", username);
        return Optional.empty();
    }

    public Optional<User> findUserInPeersById(String userId) {
        if (!peerConfig.isEnabled()) {
            return Optional.empty();
        }
        
        List<String> activePeers = peerConfig.getActivePeers();
        log.debug("Searching for user ID '{}' in {} peers", userId, activePeers.size());
        
        for (String peerUrl : activePeers) {
            try {
                String url = peerUrl + "/api/internal/users/" + userId;
                log.debug("Querying peer: {}", url);
                
                ResponseEntity<User> response = restTemplate.getForEntity(url, User.class);
                
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    log.info("Found user ID '{}' in peer: {}", userId, peerUrl);
                    return Optional.of(response.getBody());
                }
                
            } catch (HttpClientErrorException.NotFound e) {
                log.debug("User ID '{}' not found in peer: {}", userId, peerUrl);
                // Continue to next peer
            } catch (ResourceAccessException e) {
                log.warn("Failed to connect to peer {}: {}", peerUrl, e.getMessage());
                // Continue to next peer
            } catch (Exception e) {
                log.warn("Error querying peer {} for user ID '{}': {}", peerUrl, userId, e.getMessage());
                // Continue to next peer
            }
        }
        
        log.debug("User ID '{}' not found in any peer", userId);
        return Optional.empty();
    }

    public Optional<User> authenticateInPeers(String username, String password) {
        if (!peerConfig.isEnabled()) {
            return Optional.empty();
        }
        
        List<String> activePeers = peerConfig.getActivePeers();
        log.debug("Authenticating user '{}' in {} peers", username, activePeers.size());
        
        for (String peerUrl : activePeers) {
            try {
                String url = peerUrl + "/api/internal/auth/authenticate";
                
                // Create authentication request
                AuthRequest authRequest = new AuthRequest(username, password);
                HttpHeaders headers = new HttpHeaders();
                headers.set("Content-Type", "application/json");
                
                HttpEntity<AuthRequest> requestEntity = new HttpEntity<>(authRequest, headers);
                
                log.debug("Authenticating with peer: {}", url);
                
                ResponseEntity<User> response = restTemplate.exchange(
                    url, 
                    HttpMethod.POST, 
                    requestEntity, 
                    User.class
                );
                
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    log.info("Successfully authenticated user '{}' in peer: {}", username, peerUrl);
                    return Optional.of(response.getBody());
                }
                
            } catch (HttpClientErrorException.Unauthorized e) {
                log.debug("Authentication failed for user '{}' in peer: {}", username, peerUrl);
                // Continue to next peer
            } catch (ResourceAccessException e) {
                log.warn("Failed to connect to peer {}: {}", peerUrl, e.getMessage());
                // Continue to next peer
            } catch (Exception e) {
                log.warn("Error authenticating with peer {} for user '{}': {}", peerUrl, username, e.getMessage());
                // Continue to next peer
            }
        }
        
        log.debug("Authentication failed for user '{}' in all peers", username);
        return Optional.empty();
    }

    public boolean isPeerHealthy(String peerUrl) {
        try {
            String healthUrl = peerUrl + "/actuator/health";
            ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.debug("Peer {} is not healthy: {}", peerUrl, e.getMessage());
            return false;
        }
    }

    public List<String> getHealthyPeers() {
        return peerConfig.getActivePeers().stream()
                .filter(this::isPeerHealthy)
                .toList();
    }

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
