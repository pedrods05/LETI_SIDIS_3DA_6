package leti_sisdis_6.hapauth.api;

import leti_sisdis_6.hapauth.dto.LoginRequest;
import leti_sisdis_6.hapauth.dto.LoginResponse;
import leti_sisdis_6.hapauth.services.AuthService;
import leti_sisdis_6.hapauth.usermanagement.RegisterUserRequest;
import leti_sisdis_6.hapauth.usermanagement.UserIdResponse;
import leti_sisdis_6.hapauth.usermanagement.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

// added for documenting error payload
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and user registration endpoints")
public class AuthApi {

    private final AuthService authService;
    private final UserService userService;

    // Minimal error payload for 4xx responses (kept here to avoid extra files)
    @Schema(name = "ApiError", description = "Standard error response")
    public static class ApiError {
        @Schema(example = "Invalid username or password")
        public String message;
        public ApiError() {}
        public ApiError(String message) { this.message = message; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    @PostMapping("/login")
    @Operation(
        summary = "User login",
        description = "Authenticates a user and returns a JWT token along with their roles",
        responses = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(
                responseCode = "401",
                description = "Invalid credentials",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))
            )
        }
    )
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest request) {
        try {
            Authentication authentication = authService.authenticate(request.getUsername(), request.getPassword());
            String token = authService.generateToken(authentication);
            
            List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(LoginResponse.builder()
                    .token(token)
                    .roles(roles)
                    .build());
        } catch (Exception e) {
            // Keep it generic to avoid leaking whether the username exists
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("Invalid username or password"));
        }
    }

    @PostMapping("/register")
    @Operation(
        summary = "Register new user",
        description = "Creates a new user account with the specified role",
        responses = {
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "409", description = "Username already exists")
        }
    )
    public ResponseEntity<UserIdResponse> register(@RequestBody @Valid RegisterUserRequest request) {
        UserIdResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}