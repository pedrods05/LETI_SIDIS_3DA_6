package leti_sisdis_6.hapappointmentrecords.dto.output;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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