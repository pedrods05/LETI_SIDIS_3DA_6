package leti_sisdis_6.hapauth.usermanagement;

import leti_sisdis_6.hapauth.usermanagement.model.Role;
import leti_sisdis_6.hapauth.usermanagement.model.User;
import leti_sisdis_6.hapauth.usermanagement.repository.UserInMemoryRepository;
import leti_sisdis_6.hapauth.dto.RegisterUserRequest;
import leti_sisdis_6.hapauth.dto.UserIdResponse;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    private final UserInMemoryRepository userInMemoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate;

    private final List<String> peers = Arrays.asList(
            "http://localhost:8089" // instance2
    );

    public UserService(UserInMemoryRepository userInMemoryRepository,
                       PasswordEncoder passwordEncoder,
                       RestTemplate restTemplate) {
        this.userInMemoryRepository = userInMemoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.restTemplate = restTemplate;
    }

    public UserIdResponse register(RegisterUserRequest request) {
        if (userInMemoryRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }

        User user = new User();
        user.setId(UUID.randomUUID().toString().substring(0, 8));
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.valueOf(request.getRole()));

        User saved = userInMemoryRepository.save(user);

        return UserIdResponse.builder()
                .id(saved.getId())
                .username(saved.getUsername())
                .role(saved.getRole().name())
                .build();
    }

    public Object getRepositoryStats() {
        return userInMemoryRepository.getInstanceInfo();
    }

    public Optional<User> findByUsername(String username) {
        Optional<User> local = userInMemoryRepository.findByUsername(username);
        if (local.isPresent()) return local;
        return findUserInPeers(username);
    }

    public boolean existsByUsername(String username) {
        if (userInMemoryRepository.existsByUsername(username)) return true;
        return findUserInPeers(username).isPresent();
    }

    public Optional<User> findById(String id) {
        Optional<User> local = userInMemoryRepository.findById(id);
        if (local.isPresent()) return local;
        return findUserInPeersById(id);
    }

    private Optional<User> findUserInPeers(String username) {
        for (String peer : peers) {
            try {
                String url = peer + "/api/internal/users/by-username/" + username;
                User user = restTemplate.getForObject(url, User.class);
                if (user != null) return Optional.of(user);
            } catch (Exception e) {
                System.out.println("Peer " + peer + " error for username " + username + ": " + e.getMessage());
            }
        }
        return Optional.empty();
    }

    private Optional<User> findUserInPeersById(String userId) {
        for (String peer : peers) {
            try {
                String url = peer + "/api/internal/users/" + userId;
                User user = restTemplate.getForObject(url, User.class);
                if (user != null) return Optional.of(user);
            } catch (Exception e) {
                System.out.println("Peer " + peer + " error for id " + userId + ": " + e.getMessage());
            }
        }
        return Optional.empty();
    }
}
