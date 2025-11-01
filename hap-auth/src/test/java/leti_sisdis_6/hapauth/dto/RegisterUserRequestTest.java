package leti_sisdis_6.hapauth.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterUserRequestTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeFactory() {
        if (factory != null) {
            factory.close();
        }
    }

    private RegisterUserRequest buildValid() {
        RegisterUserRequest req = new RegisterUserRequest();
        req.setUsername("new@example.com");
        req.setPassword("pw");
        req.setRole("PATIENT");
        return req;
    }

    @Test
    @DisplayName("Válido quando username, password e role estão preenchidos")
    void validRequest_hasNoViolations() {
        RegisterUserRequest req = buildValid();
        Set<ConstraintViolation<RegisterUserRequest>> violations = validator.validate(req);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Username não pode ser em branco")
    void blankUsername_hasViolation() {
        RegisterUserRequest req = buildValid();
        req.setUsername(" ");
        Set<ConstraintViolation<RegisterUserRequest>> violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @Test
    @DisplayName("Password não pode ser em branco")
    void blankPassword_hasViolation() {
        RegisterUserRequest req = buildValid();
        req.setPassword("  ");
        Set<ConstraintViolation<RegisterUserRequest>> violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("Role não pode ser em branco")
    void blankRole_hasViolation() {
        RegisterUserRequest req = buildValid();
        req.setRole("");
        Set<ConstraintViolation<RegisterUserRequest>> violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("role"));
    }
}
