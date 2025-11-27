package leti_sisdis_6.hapappointmentrecords.dto.input;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class  AppointmentRecordRequestTest {

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

    private AppointmentRecordRequest buildValid() {
        AppointmentRecordRequest req = new AppointmentRecordRequest();
        req.setDiagnosis("Gripe");
        req.setTreatmentRecommendations("Repouso e hidratação");
        req.setPrescriptions("Paracetamol 1g");
        req.setDuration(LocalTime.of(0, 30));
        return req;
    }

    @Test
    @DisplayName("Deve ser válido quando todos os campos estão preenchidos corretamente")
    void validRequest_hasNoViolations() {
        AppointmentRecordRequest req = buildValid();

        Set<ConstraintViolation<AppointmentRecordRequest>> violations = validator.validate(req);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Diagnosis não pode ser em branco")
    void blankDiagnosis_hasViolation() {
        AppointmentRecordRequest req = buildValid();
        req.setDiagnosis("   ");

        Set<ConstraintViolation<AppointmentRecordRequest>> violations = validator.validate(req);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("diagnosis"));
    }

    @Test
    @DisplayName("TreatmentRecommendations não pode ser em branco")
    void blankTreatmentRecommendations_hasViolation() {
        AppointmentRecordRequest req = buildValid();
        req.setTreatmentRecommendations("");

        Set<ConstraintViolation<AppointmentRecordRequest>> violations = validator.validate(req);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("treatmentRecommendations"));
    }

    @Test
    @DisplayName("Prescriptions não pode ser em branco")
    void blankPrescriptions_hasViolation() {
        AppointmentRecordRequest req = buildValid();
        req.setPrescriptions("   ");

        Set<ConstraintViolation<AppointmentRecordRequest>> violations = validator.validate(req);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("prescriptions"));
    }

    @Test
    @DisplayName("Duration é obrigatório (NotNull)")
    void nullDuration_hasViolation() {
        AppointmentRecordRequest req = buildValid();
        req.setDuration(null);

        Set<ConstraintViolation<AppointmentRecordRequest>> violations = validator.validate(req);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("duration"));
    }
}
