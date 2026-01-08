package leti_sisdis_6.hapappointmentrecords.repository;

import leti_sisdis_6.hapappointmentrecords.model.AppointmentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppointmentRecordRepository extends JpaRepository<AppointmentRecord, String> {

    // Find record by appointmentId
    Optional<AppointmentRecord> findByAppointmentId(String appointmentId);
}
