package leti_sisdis_6.happatients.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.Data;

import java.time.LocalDate;

@Data
public class HealthConcernDTO {
    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Diagnosis date is required")
    @Past(message = "Diagnosis date must be in the past")
    private LocalDate diagnosisDate;

    private String treatment;

    @NotNull(message = "Ongoing status is required")
    private Boolean ongoing;

    private LocalDate resolvedDate;
} 