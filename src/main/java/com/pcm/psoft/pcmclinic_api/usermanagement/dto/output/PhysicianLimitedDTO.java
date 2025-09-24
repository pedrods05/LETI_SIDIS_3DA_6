package com.pcm.psoft.pcmclinic_api.usermanagement.dto.output;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhysicianLimitedDTO {
    private String fullName;
    private String specialtyId;
    private String specialtyName;
} 