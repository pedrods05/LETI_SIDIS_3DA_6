package leti_sisdis_6.happatients.repository;

import leti_sisdis_6.happatients.model.PatientEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PatientEventRepository extends JpaRepository<PatientEvent, Long> {

    List<PatientEvent> findByPatientIdOrderByOccurredAtAsc(String patientId);
}

