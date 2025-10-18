package leti_sisdis_6.hapauth.usermanagement;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
public class RegisterUserRequest {
    @NotBlank(message = "Username is required")
    private String username;
    
    @NotBlank(message = "Password is required")
    private String password;
    
    @NotBlank(message = "Role is required")
    @jakarta.validation.constraints.Pattern(
        regexp = "ADMIN|PATIENT|PHYSICIAN",
        flags = { jakarta.validation.constraints.Pattern.Flag.CASE_INSENSITIVE },
        message = "Role must be one of [ADMIN, PATIENT, PHYSICIAN]"
    )
    @Schema(example = "ADMIN", allowableValues = {"ADMIN","PATIENT","PHYSICIAN"})
    private String role;
}
