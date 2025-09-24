package com.pcm.psoft.pcmclinic_api.appointment.dto.output;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AppointmentIdResponse {
    private String appointmentId;
    private String status;
    private String message;
} 