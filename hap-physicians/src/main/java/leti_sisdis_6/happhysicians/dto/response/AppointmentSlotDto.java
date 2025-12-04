package leti_sisdis_6.happhysicians.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentSlotDto {
    private String date;
    private String startTime;
    private String endTime;
}
