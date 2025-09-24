package com.pcm.psoft.pcmclinic_api.appointment.dto.output;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhysicianAverageDTO {
    private String physicianId;
    private String physicianName;
    private String averageDuration;
} 