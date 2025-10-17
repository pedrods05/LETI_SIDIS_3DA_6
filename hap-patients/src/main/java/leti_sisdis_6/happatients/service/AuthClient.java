package leti_sisdis_6.happatients.service;

import lombok.Builder;
import lombok.Data;
import leti_sisdis_6.happatients.exceptions.ConflictException;
import leti_sisdis_6.happatients.exceptions.EmailAlreadyExistsException;
import leti_sisdis_6.happatients.exceptions.ForbiddenException;
import leti_sisdis_6.happatients.exceptions.UnauthorizedException;
import leti_sisdis_6.happatients.exceptions.UserNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class AuthClient {
    private final RestTemplate restTemplate;
    private final String authBaseUrl;

    public AuthClient(RestTemplate restTemplate,
                      @Value("${hap.auth.base-url:http://localhost:8084}") String authBaseUrl) {
        this.restTemplate = restTemplate;
        this.authBaseUrl = authBaseUrl;
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 200, multiplier = 2.0))
    public UserIdResponse registerUser(String username, String rawPassword, String role) {
        String url = authBaseUrl + "/api/public/register";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        RegisterUserRequest payload = RegisterUserRequest.builder()
                .username(username)
                .password(rawPassword)
                .role(role)
                .build();

        try {
            return restTemplate.postForObject(url, new HttpEntity<>(payload, headers), UserIdResponse.class);
        } catch (HttpClientErrorException.Conflict e) {
            throw new EmailAlreadyExistsException("Email already exists");
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new UnauthorizedException("Unauthorized to call auth service");
        } catch (HttpClientErrorException.Forbidden e) {
            throw new ForbiddenException("Forbidden calling auth service");
        } catch (HttpClientErrorException.NotFound e) {
            throw new UserNotFoundException("Auth service user endpoint not found");
        } catch (HttpClientErrorException e) {
            throw new ConflictException("Auth service error: " + e.getStatusCode());
        }
    }

    @Data
    @Builder
    private static class RegisterUserRequest {
        private String username;
        private String password;
        private String role;
    }

    @Data
    private static class UserIdResponse {
        private String id;
        private String username;
        private String role;
    }
}


