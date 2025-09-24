package com.pcm.psoft.pcmclinic_api.patient.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PatientRegistrationDTO {
    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 10)
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).+$",
             message = "Password must contain at least one uppercase letter, one number, and one special character")
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
    @NotNull(message = "Insurance information is required when provided")
    private InsuranceInfoDTO insuranceInfo;

    @NotNull(message = "Data consent is required")
    private Boolean dataConsentGiven;

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

        @NotBlank(message = "Provider is required")
        private String provider;

        @NotBlank(message = "Coverage type is required")
        private String coverageType;
    }
} 