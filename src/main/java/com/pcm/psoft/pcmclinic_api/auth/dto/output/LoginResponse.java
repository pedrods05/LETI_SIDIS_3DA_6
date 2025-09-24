package com.pcm.psoft.pcmclinic_api.auth.dto.output;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LoginResponse {
    private String token;
    private List<String> roles;
} 