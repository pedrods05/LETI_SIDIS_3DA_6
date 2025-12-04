package leti_sisdis_6.hapappointmentrecords.repository;

import leti_sisdis_6.hapappointmentrecords.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, String> {
    List<Appointment> findByPatientId(String patientId);
    List<Appointment> findByPhysicianId(String physicianId);
    List<Appointment> findByPhysicianIdAndDateTime(String physicianId, LocalDateTime dateTime);
}
