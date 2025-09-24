package com.pcm.psoft.pcmclinic_api.appointment.dto.input;

import com.pcm.psoft.pcmclinic_api.appointment.model.ConsultationType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class UpdateAppointmentRequest {
    private LocalDateTime dateTime;
    private ConsultationType consultationType;
} 