package leti_sisdis_6.happhysicians.dto.output;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhysicianLimitedDTO {
    private String fullName;
    private String specialtyId;
    private String specialtyName;
}
