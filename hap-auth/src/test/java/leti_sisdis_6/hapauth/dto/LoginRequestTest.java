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

class LoginRequestTest {

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

    private LoginRequest buildValid() {
        LoginRequest req = new LoginRequest();
        req.setUsername("john@example.com");
        req.setPassword("secret");
        return req;
    }

    @Test
    @DisplayName("Válido quando username e password estão preenchidos")
    void validRequest_hasNoViolations() {
        LoginRequest req = buildValid();
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(req);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Username não pode ser em branco")
    void blankUsername_hasViolation() {
        LoginRequest req = buildValid();
        req.setUsername("   ");
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @Test
    @DisplayName("Password não pode ser em branco")
    void blankPassword_hasViolation() {
        LoginRequest req = buildValid();
        req.setPassword("");
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(req);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }
}
