package com.pcm.psoft.pcmclinic_api.appointment.dto.output;
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
public class AppointmentDetailsDTO {
    private String appointmentId;
    private String patientId;
    private String patientName;
    private String physicianId;
    private String physicianName;
    private LocalDateTime dateTime;
    private ConsultationType consultationType;
    private AppointmentStatus status;
}

