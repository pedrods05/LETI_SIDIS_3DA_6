package leti_sisdis_6.happatients.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PatientRegistrationDTOV2 {
    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 10, message = "Password must be at least 10 characters long")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).+$",
        message = "Password must contain at least one uppercase letter, one number, and one special character"
    )
    private String password;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String phoneNumber;

    @NotNull(message = "Birth date is required")
    @Past(message = "Birth date must be in the past")
    private LocalDate birthDate;

    @Valid
    @NotNull(message = "Address is required")
    private AddressDTO address;

    @Valid
    @NotNull(message = "Insurance information is required")
    private InsuranceInfoDTO insuranceInfo;

    @NotNull(message = "Data consent is required")
    private Boolean dataConsentGiven;

    private List<@Valid HealthConcernDTO> healthConcerns;

    @Valid
    @NotNull(message = "Photo is required")
    private PhotoDTO photo;

    @Data
    public static class AddressDTO {
        @NotBlank(message = "Street is required")
        private String street;

        @NotBlank(message = "City is required")
        private String city;

        @NotBlank(message = "Postal code is required")
        private String postalCode;

        @NotBlank(message = "Country is required")
        private String country;
    }

    @Data
    public static class InsuranceInfoDTO {
        @NotBlank(message = "Policy number is required")
        private String policyNumber;

        @NotBlank(message = "Company name is required")
        private String companyName;

        @NotBlank(message = "Coverage type is required")
        private String coverageType;
    }
} 