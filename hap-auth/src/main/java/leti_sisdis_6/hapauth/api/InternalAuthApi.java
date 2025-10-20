package leti_sisdis_6.hapauth.api;

import leti_sisdis_6.hapauth.services.AuthService;
import leti_sisdis_6.hapauth.usermanagement.User;
import leti_sisdis_6.hapauth.usermanagement.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Hidden;

import java.util.Optional;

@RestController
@RequestMapping("/api/internal")
@Hidden
@RequiredArgsConstructor
public class InternalAuthApi {
    
    private final UserService userService;
    private final AuthService authService;
    
    @GetMapping("/users/by-username/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        Optional<User> user = userService.findByUsername(username);
        return user.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUserById(@PathVariable String id) {
        Optional<User> user = userService.findById(id);
        return user.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/auth/authenticate")
    public ResponseEntity<User> authenticateUser(@RequestBody AuthService.AuthRequest authRequest) {
        Optional<User> user = authService.authenticateWithPeers(authRequest.getUsername(), authRequest.getPassword());
        return user.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }
}
