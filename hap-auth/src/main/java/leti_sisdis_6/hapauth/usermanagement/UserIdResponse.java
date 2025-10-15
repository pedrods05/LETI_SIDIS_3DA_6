package leti_sisdis_6.hapauth.usermanagement;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserIdResponse {
    private String id;
    private String username;
    private String role;
}
