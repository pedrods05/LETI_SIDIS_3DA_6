package leti_sisdis_6.hapappointmentrecords.dto.output;

import com.pcm.psoft.pcmclinic_api.appointment.model.AppointmentStatus;
import com.pcm.psoft.pcmclinic_api.appointment.model.ConsultationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientAppointmentDTO {
    private String physicianName;
    private LocalDateTime dateTime;
    private ConsultationType consultationType;
    private AppointmentStatus status;
} 