package leti_sisdis_6.hapauth.usermanagement;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    private final UserInMemoryRepository userInMemoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate;
    
    // Hardcoded peer list - simple approach
    private final List<String> peers = Arrays.asList(
        "http://localhost:8085", // instance2
        "http://localhost:8086"  // instance3
    );

    public UserService(UserInMemoryRepository userInMemoryRepository, 
                      PasswordEncoder passwordEncoder,
                      RestTemplate restTemplate) {
        this.userInMemoryRepository = userInMemoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.restTemplate = restTemplate;
    }

    public UserIdResponse register(RegisterUserRequest request) {
        // Check if username already exists
        if (userInMemoryRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }
        
        User user = new User();
        user.setId(UUID.randomUUID().toString().substring(0, 5));
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.valueOf(request.getRole()));
        
        User savedUser = userInMemoryRepository.save(user);
        
        return UserIdResponse.builder()
            .id(savedUser.getId())
            .username(savedUser.getUsername())
            .role(savedUser.getRole().name())
            .build();
    }
    
    /**
     * Get repository statistics for debugging
     */
    public Object getRepositoryStats() {
        return userInMemoryRepository.getInstanceInfo();
    }
    
    /**
     * Find user by username, checking local repository first, then peers
     * @param username the username to search for
     * @return Optional<User> if found locally or in peers
     */
    public Optional<User> findByUsername(String username) {
        // Check local repository first
        Optional<User> localUser = userInMemoryRepository.findByUsername(username);
        if (localUser.isPresent()) {
            return localUser;
        }
        
        // If not found locally, check peers
        return findUserInPeers(username);
    }
    
    /**
     * Check if username exists, checking local repository first, then peers
     * @param username the username to check
     * @return true if username exists locally or in peers
     */
    public boolean existsByUsername(String username) {
        // Check local repository first
        if (userInMemoryRepository.existsByUsername(username)) {
            return true;
        }
        
        // If not found locally, check peers
        return findUserInPeers(username).isPresent();
    }
    
    /**
     * Find user by ID, checking local repository first, then peers
     * @param id the user ID to search for
     * @return Optional<User> if found locally or in peers
     */
    public Optional<User> findById(String id) {
        // Check local repository first
        Optional<User> localUser = userInMemoryRepository.findById(id);
        if (localUser.isPresent()) {
            return localUser;
        }
        
        // If not found locally, check peers
        return findUserInPeersById(id);
    }
    
    /**
     * Forward user lookup request to peers
     */
    private Optional<User> findUserInPeers(String username) {
        for (String peer : peers) {
            try {
                String url = peer + "/api/internal/users/by-username/" + username;
                User user = restTemplate.getForObject(url, User.class);
                if (user != null) {
                    return Optional.of(user);
                }
            } catch (Exception e) {
                // Log and continue to next peer
                System.out.println("Failed to query peer " + peer + " for user " + username + ": " + e.getMessage());
            }
        }
        return Optional.empty();
    }
    
    /**
     * Forward user lookup by ID request to peers
     */
    private Optional<User> findUserInPeersById(String userId) {
        for (String peer : peers) {
            try {
                String url = peer + "/api/internal/users/" + userId;
                User user = restTemplate.getForObject(url, User.class);
                if (user != null) {
                    return Optional.of(user);
                }
            } catch (Exception e) {
                // Log and continue to next peer
                System.out.println("Failed to query peer " + peer + " for user ID " + userId + ": " + e.getMessage());
            }
        }
        return Optional.empty();
    }
}
