package leti_sisdis_6.hapauth.api;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.ConstraintViolation;

import java.util.Set;

import leti_sisdis_6.hapauth.usermanagement.model.Role;

import static org.junit.jupiter.api.Assertions.*;

class CreateUserRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("CreateUserRequest válido → sem violações")
    void valid() {
        CreateUserRequest req = new CreateUserRequest();
        req.setUsername("new@example.com");
        req.setPassword("pw");
        req.setRole(Role.PATIENT);

        Set<ConstraintViolation<CreateUserRequest>> v = validator.validate(req);
        assertTrue(v.isEmpty());
    }

    @Test
    @DisplayName("CreateUserRequest com campos em falta/invalid → violações presentes")
    void invalid() {
        CreateUserRequest req = new CreateUserRequest();
        // username inválido e restantes nulos
        req.setUsername("bad");

        Set<ConstraintViolation<CreateUserRequest>> v = validator.validate(req);
        assertFalse(v.isEmpty());
    }
}
