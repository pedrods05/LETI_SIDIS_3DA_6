package leti_sisdis_6.happatients.usermanagement.dto.output;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserIdResponse {
    private String id;
    private String username;
    private String role;
} 