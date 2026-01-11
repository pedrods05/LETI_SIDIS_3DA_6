package leti_sisdis_6.hapappointmentrecords.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailAlreadyExistsExceptionTest {

    @Test
    @DisplayName("Deve criar EmailAlreadyExistsException com mensagem")
    void shouldCreateEmailAlreadyExistsExceptionWithMessage() {
        String message = "Email already exists in the system";

        EmailAlreadyExistsException exception = new EmailAlreadyExistsException(message);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Deve ser lançável como exceção")
    void shouldBeThrowableAsException() {
        String email = "test@example.com";
        String message = "Email " + email + " already exists";

        assertThatThrownBy(() -> {
            throw new EmailAlreadyExistsException(message);
        })
        .isInstanceOf(EmailAlreadyExistsException.class)
        .hasMessage(message);
    }

    @Test
    @DisplayName("Deve herdar de RuntimeException")
    void shouldExtendRuntimeException() {
        EmailAlreadyExistsException exception = new EmailAlreadyExistsException("test");

        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Deve permitir mensagem formatada com email")
    void shouldAllowFormattedMessageWithEmail() {

        String email = "user@example.com";
        String message = String.format("Email '%s' is already registered", email);

        EmailAlreadyExistsException exception = new EmailAlreadyExistsException(message);

        assertThat(exception.getMessage()).contains(email);
        assertThat(exception.getMessage()).contains("already registered");
    }

    @Test
    @DisplayName("Deve permitir mensagem nula")
    void shouldAllowNullMessage() {
        EmailAlreadyExistsException exception = new EmailAlreadyExistsException(null);
        assertThat(exception.getMessage()).isNull();
    }

    @Test
    @DisplayName("Deve ser uma exceção específica para emails duplicados")
    void shouldBeSpecificExceptionForDuplicateEmails() {
        String duplicateEmail = "duplicate@example.com";
        String message = "The email " + duplicateEmail + " is already registered in the system";

        EmailAlreadyExistsException exception = new EmailAlreadyExistsException(message);

        assertThat(exception).isInstanceOf(EmailAlreadyExistsException.class);
        assertThat(exception.getMessage()).contains("duplicate@example.com");
        assertThat(exception.getMessage()).contains("already registered");
    }

    @Test
    @DisplayName("Deve ter mensagem não nula quando criada com mensagem")
    void shouldHaveNonNullMessageWhenCreatedWithMessage() {
        String message = "Test message";

        EmailAlreadyExistsException exception = new EmailAlreadyExistsException(message);

        assertThat(exception.getMessage()).isNotNull();
        assertThat(exception.getMessage()).isNotEmpty();
        assertThat(exception.getMessage()).isEqualTo(message);
    }

    @Test
    @DisplayName("Deve manter a mensagem original sem modificações")
    void shouldPreserveOriginalMessageWithoutModifications() {
        String originalMessage = "This is the original error message for email conflict";

        EmailAlreadyExistsException exception = new EmailAlreadyExistsException(originalMessage);

        assertThat(exception.getMessage()).isEqualTo(originalMessage);
    }
}
