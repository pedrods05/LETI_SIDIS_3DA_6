package leti_sisdis_6.hapauth.setup;

import leti_sisdis_6.hapauth.usermanagement.model.Role;
import leti_sisdis_6.hapauth.usermanagement.model.User;
import leti_sisdis_6.hapauth.usermanagement.repository.UserInMemoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminBootstrapTest {

    @Test
    @DisplayName("run() → cria admin quando não existe, com password codificada e role ADMIN")
    void run_createsAdminIfMissing() throws Exception {
        UserInMemoryRepository repo = mock(UserInMemoryRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);

        when(repo.findByUsername(any())).thenReturn(Optional.empty());
        when(repo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(encoder.encode(any())).thenReturn("ENCODED");

        AdminBootstrap bootstrap = new AdminBootstrap(repo, encoder);

        bootstrap.run();

        ArgumentCaptor<User> userCap = ArgumentCaptor.forClass(User.class);
        verify(repo, atLeastOnce()).save(userCap.capture());

        User saved = userCap.getValue();
        assertNotNull(saved, "User guardado não deve ser nulo");
        assertEquals(Role.ADMIN, saved.getRole(), "Role deve ser ADMIN");
        assertEquals("ENCODED", saved.getPassword(), "Password deve ser codificada");
        assertNotNull(saved.getId(), "ID deve ser preenchido");
        assertTrue(saved.getId().startsWith("ADM"), "ID deve começar por 'ADM'");
        assertNotNull(saved.getUsername(), "Username deve ser definido");
    }

    @Test
    @DisplayName("run() → não cria novo admin quando já existe")
    void run_doesNotCreateIfAlreadyExists() throws Exception {
        // Arrange
        UserInMemoryRepository repo = mock(UserInMemoryRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);

        User existing = new User();
        existing.setId("ADM01");
        existing.setUsername("admin@example.com");
        existing.setRole(Role.ADMIN);

        when(repo.findByUsername(any())).thenReturn(Optional.of(existing));

        AdminBootstrap bootstrap = new AdminBootstrap(repo, encoder);

        bootstrap.run();

        verify(repo, never()).save(any(User.class));
        verify(repo, atLeastOnce()).findByUsername(any());
    }
}
