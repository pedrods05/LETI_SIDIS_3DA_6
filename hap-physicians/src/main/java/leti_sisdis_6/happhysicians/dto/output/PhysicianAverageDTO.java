package leti_sisdis_6.happhysicians.dto.output;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhysicianAverageDTO {
    private String physicianId;
    private String physicianName;
    private String averageDuration;
}
