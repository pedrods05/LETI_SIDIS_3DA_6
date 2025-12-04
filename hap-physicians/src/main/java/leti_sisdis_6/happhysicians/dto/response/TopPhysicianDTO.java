package leti_sisdis_6.happhysicians.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopPhysicianDTO {
    private String physicianId;
    private String fullName;
    private String specialtyName;
    private Long appointmentCount;
}
