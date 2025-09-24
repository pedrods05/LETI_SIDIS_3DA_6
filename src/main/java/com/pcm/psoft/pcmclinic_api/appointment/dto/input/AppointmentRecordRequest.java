package com.pcm.psoft.pcmclinic_api.appointment.dto.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

@Data
public class AppointmentRecordRequest {
    @NotBlank(message = "Diagnosis is required")
    private String diagnosis;

    @NotBlank(message = "Treatment recommendations are required")
    private String treatmentRecommendations;

    @NotBlank(message = "Prescriptions are required")
    private String prescriptions;

    @NotNull(message = "Duration is required")
    private LocalTime duration;
} 