package leti_sisdis_6.hapauth.api;

import leti_sisdis_6.hapauth.exceptions.UserNotFoundException;
import leti_sisdis_6.hapauth.usermanagement.model.User;
import leti_sisdis_6.hapauth.usermanagement.repository.UserInMemoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthHelper {

    private final UserInMemoryRepository userInMemoryRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("No authenticated user found");
        }
        return userInMemoryRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new UserNotFoundException(authentication.getName()));
    }

    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null &&
               authentication.getAuthorities().stream()
                   .anyMatch(a -> a.getAuthority().equals("ADMIN"));
    }

    public boolean isPatient() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null &&
               authentication.getAuthorities().stream()
                   .anyMatch(a -> a.getAuthority().equals("PATIENT"));
    }
}
