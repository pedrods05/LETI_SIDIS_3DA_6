package com.pcm.psoft.pcmclinic_api.usermanagement.services;

import com.pcm.psoft.pcmclinic_api.usermanagement.dto.input.RegisterUserRequest;
import com.pcm.psoft.pcmclinic_api.usermanagement.dto.output.UserIdResponse;
import com.pcm.psoft.pcmclinic_api.usermanagement.model.User;
import com.pcm.psoft.pcmclinic_api.usermanagement.model.Role;
import com.pcm.psoft.pcmclinic_api.usermanagement.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserIdResponse register(RegisterUserRequest request) {
        User user = new User();
        user.setId(UUID.randomUUID().toString().substring(0, 5));
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.valueOf(request.getRole()));
        
        User savedUser = userRepository.save(user);
        
        return UserIdResponse.builder()
            .id(savedUser.getId())
            .username(savedUser.getUsername())
            .role(savedUser.getRole().name())
            .build();
    }
} 