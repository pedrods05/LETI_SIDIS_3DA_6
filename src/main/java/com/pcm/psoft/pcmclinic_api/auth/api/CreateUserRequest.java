package com.pcm.psoft.pcmclinic_api.auth.api;

import com.pcm.psoft.pcmclinic_api.usermanagement.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateUserRequest {

    @NotNull
    @Email
    private String username;

    @NotNull
    private String password;

    @NotNull
    private Role role;
}
