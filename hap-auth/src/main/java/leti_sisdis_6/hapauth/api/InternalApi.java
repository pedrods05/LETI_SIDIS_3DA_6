package leti_sisdis_6.hapauth.api;

import leti_sisdis_6.hapauth.services.PeerService;
import leti_sisdis_6.hapauth.usermanagement.User;
import leti_sisdis_6.hapauth.usermanagement.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
@Tag(name = "Internal API", description = "Internal endpoints for peer communication")
public class InternalApi {
    
    private final UserService userService;
    private final PeerService peerService;
    
    @GetMapping("/users/by-username/{username}")
    @Operation(summary = "Get user by username", description = "Internal endpoint for peer user lookup")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        Optional<User> user = userService.findByUsername(username);
        return user.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/users/{id}")
    @Operation(summary = "Get user by ID", description = "Internal endpoint for peer user lookup")
    public ResponseEntity<User> getUserById(@PathVariable String id) {
        Optional<User> user = userService.findById(id);
        return user.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/auth/authenticate")
    @Operation(summary = "Authenticate user", description = "Internal endpoint for peer authentication")
    public ResponseEntity<User> authenticateUser(@RequestBody PeerService.AuthRequest authRequest) {
        Optional<User> user = peerService.authenticateInPeers(authRequest.getUsername(), authRequest.getPassword());
        return user.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/peers/health")
    @Operation(summary = "Get peer health status", description = "Internal endpoint for peer health checking")
    public ResponseEntity<Object> getPeerHealth() {
        return ResponseEntity.ok(peerService.getHealthyPeers());
    }
}
