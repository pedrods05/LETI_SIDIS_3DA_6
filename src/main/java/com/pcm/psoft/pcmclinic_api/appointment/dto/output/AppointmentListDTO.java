package com.pcm.psoft.pcmclinic_api.appointment.dto.output;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentListDTO {
    private LocalDate date;
    private LocalTime time;
    private String patientName;
    private String physicianName;
}
