package leti_sisdis_6.hapappointmentrecords.dto.output;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentSlotDto {
    private String date;
    private String startTime;
    private String endTime;
} 