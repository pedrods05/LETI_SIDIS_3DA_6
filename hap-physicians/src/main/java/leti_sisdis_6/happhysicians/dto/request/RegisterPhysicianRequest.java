package leti_sisdis_6.happhysicians.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class RegisterPhysicianRequest {
    
    @NotBlank(message = "Full name is required")
    private String fullName;
    
    @NotBlank(message = "License number is required")
    private String licenseNumber;
    
    @NotBlank(message = "Username is required")
    private String username;
    
    @NotBlank(message = "Password is required")
    private String password;
    
    @NotBlank(message = "Specialty ID is required")
    private String specialtyId;
    
    @NotBlank(message = "Department ID is required")
    private String departmentId;
    
    private List<String> emails;
    private List<String> phoneNumbers;
    
    @NotNull(message = "Working hour start is required")
    private String workingHourStart;
    
    @NotNull(message = "Working hour end is required")
    private String workingHourEnd;
}
