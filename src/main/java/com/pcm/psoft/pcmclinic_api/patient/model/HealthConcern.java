package com.pcm.psoft.pcmclinic_api.patient.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "health_concerns")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthConcern {
    @Id
    private String id;
    private String description;
    private LocalDate diagnosisDate;
    private String treatment;
    private Boolean ongoing;
    private LocalDate resolvedDate;
} 