package leti_sisdis_6.hapappointmentrecords.repository;

import leti_sisdis_6.hapappointmentrecords.model.AppointmentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRecordRepository extends JpaRepository<AppointmentRecord, String> {
    Optional<AppointmentRecord> findByAppointmentId(String appointmentId);
    List<AppointmentRecord> findByAppointmentIdIn(List<String> appointmentIds);
} 