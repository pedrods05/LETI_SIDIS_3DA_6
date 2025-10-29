package leti_sisdis_6.hapauth.setup;

import leti_sisdis_6.hapauth.usermanagement.model.Role;
import leti_sisdis_6.hapauth.usermanagement.model.User;
import leti_sisdis_6.hapauth.usermanagement.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminBootstrap implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private int adminCounter = 1;

    @Override
    public void run(String... args) {
        createAdminIfNotExists("pedrinho@clinic.pt", "12345678");
        createAdminIfNotExists("pedrao@clinic.pt", "12345678");
        createAdminIfNotExists("lulu@clinic.pt", "12345678");
        createAdminIfNotExists("zelao@clinic.pt", "12345678");
    }

    private void createAdminIfNotExists(String email, String rawPassword) {
        userRepository.findByUsername(email).orElseGet(() -> {
            User user = new User();
            user.setId(generateNextAdminId());
            user.setUsername(email);
            user.setPassword(passwordEncoder.encode(rawPassword));
            user.setRole(Role.ADMIN);
            return userRepository.save(user);
        });
    }

    private String generateNextAdminId() {
        return String.format("ADM%02d", adminCounter++);
    }
}
