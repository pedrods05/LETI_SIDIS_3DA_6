package leti_sisdis_6.hapauth.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Não há anotações de validação em LoginResponse, por isso testamos construção e getters.
 */
class LoginResponseTest {

    @Test
    @DisplayName("Builder deve construir com token e roles")
    void builder_shouldBuild() {
        LoginResponse resp = LoginResponse.builder()
                .token("token-123")
                .roles(List.of("ADMIN", "PATIENT"))
                .build();

        assertThat(resp.getToken()).isEqualTo("token-123");
        assertThat(resp.getRoles()).containsExactly("ADMIN", "PATIENT");
    }
}
