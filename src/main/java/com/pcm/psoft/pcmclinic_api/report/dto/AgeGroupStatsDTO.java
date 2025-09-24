package com.pcm.psoft.pcmclinic_api.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AgeGroupStatsDTO {
    private String ageGroup;
    private int appointmentCount;
    private int averageDuration; // em minutos
    private double averageAppointmentsPerPatient;
} 