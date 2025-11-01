package leti_sisdis_6.hapauth.api;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.ConstraintViolation;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AuthRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("AuthRequest válido → sem violações")
    void valid() {
        AuthRequest req = new AuthRequest("john@example.com", "pw");
        Set<ConstraintViolation<AuthRequest>> v = validator.validate(req);
        assertTrue(v.isEmpty());
    }

    @Test
    @DisplayName("AuthRequest com email inválido e password null → violações presentes")
    void invalidEmailAndNulls() {
        AuthRequest req = new AuthRequest("not-an-email", null);
        Set<ConstraintViolation<AuthRequest>> v = validator.validate(req);
        assertFalse(v.isEmpty());
    }
}
