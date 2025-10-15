package leti_sisdis_6.hapappointmentrecords.dto.output;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentRecordResponse {
    private String message;
    private String appointmentId;
    private String recordId;
} 