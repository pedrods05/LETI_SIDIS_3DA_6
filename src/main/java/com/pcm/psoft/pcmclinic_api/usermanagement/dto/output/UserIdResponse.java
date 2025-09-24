package com.pcm.psoft.pcmclinic_api.usermanagement.dto.output;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserIdResponse {
    private String id;
    private String username;
    private String role;
} 