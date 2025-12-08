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
        String message = "Resource conflict detected";

        ConflictException exception = new ConflictException(message);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Deve criar ConflictException com mensagem e MalformedURLException")
    void shouldCreateConflictExceptionWithMessageAndMalformedURLException() {
        String message = "URL conflict detected";
        MalformedURLException cause = new MalformedURLException("Invalid URL format");

        ConflictException exception = new ConflictException(message, cause);

        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("Deve criar ConflictException com Class e long id")
    void shouldCreateConflictExceptionWithClassAndLongId() {
        Class<?> clazz = String.class;
        long id = 123L;

        ConflictException exception = new ConflictException(clazz, id);

        assertThat(exception.getMessage()).contains("String");
        assertThat(exception.getMessage()).contains("123");
        assertThat(exception.getMessage()).contains("not found");
    }

    @Test
    @DisplayName("Deve criar ConflictException com Class e String id")
    void shouldCreateConflictExceptionWithClassAndStringId() {
        Class<?> clazz = String.class;
        String id = "ABC123";

        ConflictException exception = new ConflictException(clazz, id);

        assertThat(exception.getMessage()).contains("String");
        assertThat(exception.getMessage()).contains("ABC123");
        assertThat(exception.getMessage()).contains("not found");
    }

    @Test
    @DisplayName("Deve ser lançável como exceção")
    void shouldBeThrowableAsException() {
        String message = "Conflict error";

        assertThatThrownBy(() -> {
            throw new ConflictException(message);
        })
        .isInstanceOf(ConflictException.class)
        .hasMessage(message);
    }

    @Test
    @DisplayName("Deve herdar de RuntimeException")
    void shouldExtendRuntimeException() {
        ConflictException exception = new ConflictException("test");

        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Deve permitir mensagem nula")
    void shouldAllowNullMessage() {
        ConflictException exception = new ConflictException(null);

        assertThat(exception.getMessage()).isNull();
    }

    @Test
    @DisplayName("Deve ter anotação ResponseStatus com CONFLICT")
    void shouldHaveResponseStatusAnnotationWithConflict() {
        ConflictException exception = new ConflictException("test");

        assertThat(exception.getClass().isAnnotationPresent(org.springframework.web.bind.annotation.ResponseStatus.class))
                .isTrue();
    }

    @Test
    @DisplayName("Deve formatar mensagem corretamente para entidade com ID long")
    void shouldFormatMessageCorrectlyForEntityWithLongId() {
        Class<?> entityClass = ConflictException.class;
        long entityId = 42L;

        ConflictException exception = new ConflictException(entityClass, entityId);

        String expectedMessage = "Entity ConflictException with id 42 not found";
        assertThat(exception.getMessage()).isEqualTo(expectedMessage);
    }

    @Test
    @DisplayName("Deve formatar mensagem corretamente para entidade com ID String")
    void shouldFormatMessageCorrectlyForEntityWithStringId() {
        Class<?> entityClass = ConflictException.class;
        String entityId = "TEST123";

        ConflictException exception = new ConflictException(entityClass, entityId);

        String expectedMessage = "Entity ConflictException with id TEST123 not found";
        assertThat(exception.getMessage()).isEqualTo(expectedMessage);
    }
}
