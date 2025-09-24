package com.pcm.psoft.pcmclinic_api.usermanagement.dto.input;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.util.List;

@Getter @Setter
public class RegisterPhysicianRequest {

    @NotBlank
    private String fullName;

    @NotBlank
    private String licenseNumber;

    @Email
    @NotBlank
    private String username;

    @NotBlank
    @Size(min = 10)
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).+$",
             message = "Password must contain at least one uppercase letter, one number, and one special character")
    private String password;

    @NotBlank
    private String specialtyId;

    @NotBlank
    private String departmentId;

    @NotEmpty
    private List<@Email String> emails;

    @NotEmpty
    private List<@Pattern(regexp = "\\+\\d{9,15}") String> phoneNumbers;

    @NotNull
    private LocalTime workingHourStart;

    @NotNull
    private LocalTime workingHourEnd;
}
