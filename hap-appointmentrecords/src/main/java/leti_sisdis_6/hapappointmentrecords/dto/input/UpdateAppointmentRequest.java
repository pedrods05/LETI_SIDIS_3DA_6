package leti_sisdis_6.hapappointmentrecords.dto.input;

import leti_sisdis_6.hapappointmentrecords.model.ConsultationType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class UpdateAppointmentRequest {
    private LocalDateTime dateTime;
    private ConsultationType consultationType;
} 