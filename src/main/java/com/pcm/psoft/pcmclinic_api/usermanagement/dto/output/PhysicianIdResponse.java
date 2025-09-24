package com.pcm.psoft.pcmclinic_api.usermanagement.dto.output;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
public class PhysicianIdResponse {
    private String physicianId;
    private String message;
    private PhotoDTO photo;

    @Getter @Setter @AllArgsConstructor @NoArgsConstructor
    public static class PhotoDTO {
        private String url;
        private java.time.LocalDateTime uploadedAt;
    }
} 