package leti_sisdis_6.hapappointmentrecords.dto.local;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

class UserDTOTest {

    @Test
    @DisplayName("Deve criar UserDTO com todos os campos")
    void createUserDTO_withAllFields() {
        // Given
        String id = "user123";
        String email = "user@example.com";
        String role = "PHYSICIAN";

        // When
        UserDTO userDTO = UserDTO.builder()
                .id(id)
                .email(email)
                .role(role)
                .build();

        // Then
        assertThat(userDTO.getId()).isEqualTo(id);
        assertThat(userDTO.getEmail()).isEqualTo(email);
        assertThat(userDTO.getRole()).isEqualTo(role);
    }

    @Test
    @DisplayName("Deve criar UserDTO usando construtor padrão")
    void createUserDTO_withDefaultConstructor() {
        // When
        UserDTO userDTO = new UserDTO();

        // Then
        assertThat(userDTO.getId()).isNull();
        assertThat(userDTO.getEmail()).isNull();
        assertThat(userDTO.getRole()).isNull();
    }

    @Test
    @DisplayName("Deve criar UserDTO usando construtor com argumentos")
    void createUserDTO_withAllArgsConstructor() {
        // Given
        String id = "user456";
        String email = "doctor@clinic.com";
        String role = "DOCTOR";

        // When
        UserDTO userDTO = new UserDTO(id, email, role);

        // Then
        assertThat(userDTO.getId()).isEqualTo(id);
        assertThat(userDTO.getEmail()).isEqualTo(email);
        assertThat(userDTO.getRole()).isEqualTo(role);
    }

    @Test
    @DisplayName("Deve permitir modificar campos usando setters")
    void modifyUserDTO_usingSetters() {
        // Given
        UserDTO userDTO = new UserDTO();
        String newId = "newId";
        String newEmail = "new@email.com";
        String newRole = "ADMIN";

        // When
        userDTO.setId(newId);
        userDTO.setEmail(newEmail);
        userDTO.setRole(newRole);

        // Then
        assertThat(userDTO.getId()).isEqualTo(newId);
        assertThat(userDTO.getEmail()).isEqualTo(newEmail);
        assertThat(userDTO.getRole()).isEqualTo(newRole);
    }

    @Test
    @DisplayName("Deve implementar equals e hashCode corretamente")
    void testEqualsAndHashCode() {
        // Given
        UserDTO user1 = UserDTO.builder()
                .id("123")
                .email("test@test.com")
                .role("USER")
                .build();

        UserDTO user2 = UserDTO.builder()
                .id("123")
                .email("test@test.com")
                .role("USER")
                .build();

        UserDTO user3 = UserDTO.builder()
                .id("456")
                .email("other@test.com")
                .role("ADMIN")
                .build();

        // Then
        assertThat(user1).isEqualTo(user2);
        assertThat(user1).isNotEqualTo(user3);
        assertThat(user1.hashCode()).isEqualTo(user2.hashCode());
    }

    @Test
    @DisplayName("Deve implementar toString corretamente")
    void testToString() {
        // Given
        UserDTO userDTO = UserDTO.builder()
                .id("123")
                .email("test@example.com")
                .role("PHYSICIAN")
                .build();

        // When
        String toString = userDTO.toString();

        // Then
        assertThat(toString).contains("id=123");
        assertThat(toString).contains("email=test@example.com");
        assertThat(toString).contains("role=PHYSICIAN");
    }

    @Test
    @DisplayName("Deve permitir campos nulos")
    void allowNullFields() {
        // When
        UserDTO userDTO = UserDTO.builder()
                .id(null)
                .email(null)
                .role(null)
                .build();

        // Then
        assertThat(userDTO.getId()).isNull();
        assertThat(userDTO.getEmail()).isNull();
        assertThat(userDTO.getRole()).isNull();
    }
}
