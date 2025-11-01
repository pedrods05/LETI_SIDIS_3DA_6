package leti_sisdis_6.hapauth.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UserIdResponse é @Value + @Builder; testamos construção e imutabilidade básica.
 */
class UserIdResponseTest {

    @Test
    @DisplayName("Builder deve construir com id, username e role")
    void builder_shouldBuild() {
        UserIdResponse resp = UserIdResponse.builder()
                .id("u1")
                .username("john@example.com")
                .role("PATIENT")
                .build();

        assertThat(resp.getId()).isEqualTo("u1");
        assertThat(resp.getUsername()).isEqualTo("john@example.com");
        assertThat(resp.getRole()).isEqualTo("PATIENT");
    }
}
