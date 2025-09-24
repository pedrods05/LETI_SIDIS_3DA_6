package com.pcm.psoft.pcmclinic_api.appointment.dto.output;

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