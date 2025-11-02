package leti_sisdis_6.hapauth.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserNotFoundExceptionTest {

    @Test
    @DisplayName("Deve estender RuntimeException")
    void shouldExtendRuntimeException() {
        assertTrue(RuntimeException.class.isAssignableFrom(UserNotFoundException.class));
    }

    @Test
    @DisplayName("Construtor deve guardar a mensagem")
    void constructorShouldStoreMessage() {
        String msg = "User with id u1 not found";
        UserNotFoundException ex = new UserNotFoundException(msg);
        assertEquals(msg, ex.getMessage());
    }

    @Test
    @DisplayName("Pode ser lançada e capturada como RuntimeException")
    void shouldBeThrowable() {
        String msg = "not found";
        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> { throw new UserNotFoundException(msg); }
        );
        assertEquals(msg, thrown.getMessage());
    }
}
