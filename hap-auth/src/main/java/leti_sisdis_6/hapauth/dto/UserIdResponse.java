package leti_sisdis_6.hapauth.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserIdResponse {
    String id;
    String username;
    String role;
}
