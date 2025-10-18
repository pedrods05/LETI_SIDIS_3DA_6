package leti_sisdis_6.happhysicians.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhysicianIdResponse {
    private String physicianId;
    private String message;
    private PhotoDTO photo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PhotoDTO {
        private String url;
        private LocalDateTime uploadedAt;
    }
}
