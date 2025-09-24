package com.pcm.psoft.pcmclinic_api.patient.service;

import com.pcm.psoft.pcmclinic_api.patient.repository.PatientRepository;
import org.springframework.stereotype.Service;

@Service
public class PatientIdGenerator {
    private final PatientRepository patientRepository;
    private int patientCounter = 1;

    public PatientIdGenerator(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public String generateNextPatientId() {
        String id = String.format("PAT%02d", patientCounter);
        patientCounter++;
        return id;
    }
} 