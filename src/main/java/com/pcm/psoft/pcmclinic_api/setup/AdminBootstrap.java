package com.pcm.psoft.pcmclinic_api.setup;

import com.pcm.psoft.pcmclinic_api.usermanagement.model.Role;
import com.pcm.psoft.pcmclinic_api.usermanagement.model.User;
import com.pcm.psoft.pcmclinic_api.usermanagement.repository.UserRepository;
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
        createAdminIfNotExists("PedroSousa@clinic.pt", "1230773");
        createAdminIfNotExists("PedroCunha@clinic.pt", "1231690");
        createAdminIfNotExists("MartimFerreira@clinic.pt", "1230850");
    }

    private void createAdminIfNotExists(String email, String rawPassword) {
        userRepository.findByUsername(email).orElseGet(() -> {
            User user = new User();
            String adminId = generateNextAdminId();
            user.setId(adminId);
            user.setUsername(email);
            user.setPassword(passwordEncoder.encode(rawPassword));
            user.setRole(Role.ADMIN);
            return userRepository.save(user);
        });
    }

    private String generateNextAdminId() {
        String id = String.format("ADM%02d", adminCounter);
        adminCounter++;
        return id;
    }
}
