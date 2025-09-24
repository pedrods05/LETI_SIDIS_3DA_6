package com.pcm.psoft.pcmclinic_api.appointment.dto.output;

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