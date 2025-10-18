package leti_sisdis_6.hapauth.api;

import leti_sisdis_6.hapauth.dto.LoginRequest;
import leti_sisdis_6.hapauth.dto.LoginResponse;
import leti_sisdis_6.hapauth.services.AuthService;
import leti_sisdis_6.hapauth.usermanagement.RegisterUserRequest;
import leti_sisdis_6.hapauth.usermanagement.UserIdResponse;
import leti_sisdis_6.hapauth.usermanagement.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and user registration endpoints")
public class AuthApi {

    // Nested error DTO to avoid a separate file
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiError {
        @Schema(example = "Invalid request data")
        private String message;

        @Schema(example = "[\"username must not be blank\", \"role must be one of [ADMIN, PATIENT, PHYSICIAN]\"]")
        private List<String> details;
    }

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/login")
    @Operation(
        summary = "User login",
        description = "Authenticates a user and returns a JWT token along with their roles",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Login successful",
                content = @Content(schema = @Schema(implementation = LoginResponse.class))
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Bad Request",
                content = @Content(schema = @Schema(implementation = AuthApi.ApiError.class))
            ),
            @ApiResponse(
                responseCode = "401",
                description = "Invalid credentials",
                content = @Content(schema = @Schema(implementation = AuthApi.ApiError.class))
            ),
            @ApiResponse(
                responseCode = "403",
                description = "Forbidden",
                content = @Content(schema = @Schema(implementation = AuthApi.ApiError.class))
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Not Found",
                content = @Content(schema = @Schema(implementation = AuthApi.ApiError.class))
            )
        }
    )
    public ResponseEntity<Object> login(@RequestBody @Valid LoginRequest request) {
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
            AuthApi.ApiError error = AuthApi.ApiError.builder()
                .message("Invalid credentials")
                .details(java.util.List.of("Username or password is incorrect"))
                .build();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @PostMapping("/register")
    @Operation(
        summary = "Register new user",
        description = "Creates a new user account with the specified role",
        responses = {
            @ApiResponse(
                responseCode = "201",
                description = "User registered successfully",
                content = @Content(schema = @Schema(implementation = UserIdResponse.class))
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid request data",
                content = @Content(schema = @Schema(implementation = AuthApi.ApiError.class))
            ),
            @ApiResponse(
                responseCode = "403",
                description = "Forbidden",
                content = @Content(schema = @Schema(implementation = AuthApi.ApiError.class))
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Not Found",
                content = @Content(schema = @Schema(implementation = AuthApi.ApiError.class))
            ),
            @ApiResponse(
                responseCode = "409",
                description = "Username already exists",
                content = @Content(schema = @Schema(implementation = AuthApi.ApiError.class))
            )
        }
    )
    public ResponseEntity<UserIdResponse> register(@RequestBody @Valid RegisterUserRequest request) {
        UserIdResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
