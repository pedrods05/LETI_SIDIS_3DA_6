package com.pcm.psoft.pcmclinic_api.patient.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "insurance_info")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsuranceInfo {
    @Id
    @Column(length = 5)
    private String id;

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false)
    private String policyNumber;

    @Column(nullable = false)
    private String coverageType;
} 