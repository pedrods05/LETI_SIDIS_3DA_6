package com.pcm.psoft.pcmclinic_api.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyReportDTO {
    private int totalAppointments;
    private int cancelled;
    private int rescheduled;
} 