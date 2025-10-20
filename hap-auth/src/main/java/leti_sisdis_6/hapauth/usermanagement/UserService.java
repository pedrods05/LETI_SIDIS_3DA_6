package leti_sisdis_6.hapauth.usermanagement;

import leti_sisdis_6.hapauth.services.PeerService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    private final UserInMemoryRepository userInMemoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final PeerService peerService;

    public UserService(UserInMemoryRepository userInMemoryRepository, 
                      PasswordEncoder passwordEncoder,
                      PeerService peerService) {
        this.userInMemoryRepository = userInMemoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.peerService = peerService;
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
        return peerService.findUserInPeers(username);
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
        return peerService.findUserInPeers(username).isPresent();
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
        return peerService.findUserInPeersById(id);
    }
}
