package leti_sisdis_6.hapauth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LoginResponse {
    private String token;
    private List<String> roles;
} 