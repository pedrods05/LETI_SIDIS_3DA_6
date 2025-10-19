package leti_sisdis_6.happatients.repository;

import leti_sisdis_6.happatients.dto.PatientDetailsDTO;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class PatientLocalRepository {
    private final Map<String, PatientDetailsDTO> store = new ConcurrentHashMap<>();

    public Optional<PatientDetailsDTO> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public PatientDetailsDTO save(PatientDetailsDTO patient) {
        if (patient == null || patient.getPatientId() == null) {
            throw new IllegalArgumentException("Patient or patientId cannot be null");
        }
        store.put(patient.getPatientId(), patient);
        return patient;
    }

    public List<PatientDetailsDTO> findAll() {
        return new ArrayList<>(store.values());
    }

    public void clear() {
        store.clear();
    }
}

