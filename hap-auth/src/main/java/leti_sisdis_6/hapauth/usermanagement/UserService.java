package leti_sisdis_6.hapauth.usermanagement;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService {
    private final UserInMemoryRepository userInMemoryRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserInMemoryRepository userInMemoryRepository, PasswordEncoder passwordEncoder) {
        this.userInMemoryRepository = userInMemoryRepository;
        this.passwordEncoder = passwordEncoder;
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
}
