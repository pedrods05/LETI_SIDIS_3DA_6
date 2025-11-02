package leti_sisdis_6.hapappointmentrecords.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConflictExceptionTest {

    @Test
    @DisplayName("Deve criar ConflictException com mensagem")
    void shouldCreateConflictExceptionWithMessage() {
        // Given
        String message = "Resource conflict detected";

        // When
        ConflictException exception = new ConflictException(message);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Deve criar ConflictException com mensagem e MalformedURLException")
    void shouldCreateConflictExceptionWithMessageAndMalformedURLException() {
        // Given
        String message = "URL conflict detected";
        MalformedURLException cause = new MalformedURLException("Invalid URL format");

        // When
        ConflictException exception = new ConflictException(message, cause);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("Deve criar ConflictException com Class e long id")
    void shouldCreateConflictExceptionWithClassAndLongId() {
        // Given
        Class<?> clazz = String.class;
        long id = 123L;

        // When
        ConflictException exception = new ConflictException(clazz, id);

        // Then
        assertThat(exception.getMessage()).contains("String");
        assertThat(exception.getMessage()).contains("123");
        assertThat(exception.getMessage()).contains("not found");
    }

    @Test
    @DisplayName("Deve criar ConflictException com Class e String id")
    void shouldCreateConflictExceptionWithClassAndStringId() {
        // Given
        Class<?> clazz = String.class;
        String id = "ABC123";

        // When
        ConflictException exception = new ConflictException(clazz, id);

        // Then
        assertThat(exception.getMessage()).contains("String");
        assertThat(exception.getMessage()).contains("ABC123");
        assertThat(exception.getMessage()).contains("not found");
    }

    @Test
    @DisplayName("Deve ser lançável como exceção")
    void shouldBeThrowableAsException() {
        // Given
        String message = "Conflict error";

        // When & Then
        assertThatThrownBy(() -> {
            throw new ConflictException(message);
        })
        .isInstanceOf(ConflictException.class)
        .hasMessage(message);
    }

    @Test
    @DisplayName("Deve herdar de RuntimeException")
    void shouldExtendRuntimeException() {
        // Given
        ConflictException exception = new ConflictException("test");

        // Then
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Deve permitir mensagem nula")
    void shouldAllowNullMessage() {
        // When
        ConflictException exception = new ConflictException(null);

        // Then
        assertThat(exception.getMessage()).isNull();
    }

    @Test
    @DisplayName("Deve ter anotação ResponseStatus com CONFLICT")
    void shouldHaveResponseStatusAnnotationWithConflict() {
        // When
        ConflictException exception = new ConflictException("test");

        // Then
        assertThat(exception.getClass().isAnnotationPresent(org.springframework.web.bind.annotation.ResponseStatus.class))
                .isTrue();
    }

    @Test
    @DisplayName("Deve formatar mensagem corretamente para entidade com ID long")
    void shouldFormatMessageCorrectlyForEntityWithLongId() {
        // Given
        Class<?> entityClass = ConflictException.class;
        long entityId = 42L;

        // When
        ConflictException exception = new ConflictException(entityClass, entityId);

        // Then
        String expectedMessage = "Entity ConflictException with id 42 not found";
        assertThat(exception.getMessage()).isEqualTo(expectedMessage);
    }

    @Test
    @DisplayName("Deve formatar mensagem corretamente para entidade com ID String")
    void shouldFormatMessageCorrectlyForEntityWithStringId() {
        // Given
        Class<?> entityClass = ConflictException.class;
        String entityId = "TEST123";

        // When
        ConflictException exception = new ConflictException(entityClass, entityId);

        // Then
        String expectedMessage = "Entity ConflictException with id TEST123 not found";
        assertThat(exception.getMessage()).isEqualTo(expectedMessage);
    }
}
