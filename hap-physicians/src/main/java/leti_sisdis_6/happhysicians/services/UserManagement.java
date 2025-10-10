package leti_sisdis_6.happhysicians.services;

import leti_sisdis_6.happhysicians.auth.CreateUserRequest;
import leti_sisdis_6.happhysicians.model.Role;
import leti_sisdis_6.happhysicians.model.User;
import leti_sisdis_6.happhysicians.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserManagement {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User create(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already in use");
        }

        Role role = request.getRole();
        String id = generateUserId(role);

        User user = new User();
        user.setId(id);
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);

        return userRepository.save(user);
    }

    private String generateUserId(Role role) {
        String prefix = switch (role) {
            case ADMIN -> "ADM";
            case PATIENT -> "PAT";
            case PHYSICIAN -> "PHY";
        };

        List<String> existingIds = userRepository.findAll().stream()
                .map(User::getId)
                .filter(id -> id.startsWith(prefix))
                .toList();

        int max = existingIds.stream()
                .map(id -> id.substring(3))
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(0);

        return String.format("%s%02d", prefix, max + 1);
    }
}
