package leti_sisdis_6.hapappointmentrecords.repository;

import leti_sisdis_6.hapappointmentrecords.model.Appointment;
import leti_sisdis_6.hapappointmentrecords.model.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, String> {
    boolean existsByPhysician_PhysicianIdAndDateTime(String physicianId, LocalDateTime dateTime);
    List<Appointment> findByPatient_PatientId(String patientId);
    List<Appointment> findByPatient_PatientIdOrderByDateTimeDesc(String patientId);
    List<Appointment> findByDateTimeAfterOrderByDateTimeAsc(LocalDateTime dateTime);
    List<Appointment> findByPhysician_PhysicianIdAndDateTimeBetween(String physicianId, java.time.LocalDateTime start, java.time.LocalDateTime end);
    List<Appointment> findByDateTimeBetween(LocalDateTime start, LocalDateTime end);

    List<Appointment> findByPatient_PatientIdAndStatus(String patientId, AppointmentStatus status);

    @Query("SELECT a.physician.physicianId as physicianId, " +
           "a.physician.fullName as physicianName, " +
           "AVG(EXTRACT(HOUR FROM ar.duration) * 60 + EXTRACT(MINUTE FROM ar.duration)) as averageDuration " +
           "FROM Appointment a " +
           "JOIN AppointmentRecord ar ON a.appointmentId = ar.appointment.appointmentId " +
           "GROUP BY a.physician.physicianId, a.physician.fullName")
    List<Object[]> findAverageDurationPerPhysician();

    @Query("SELECT a.physician.physicianId, a.physician.fullName, a.physician.specialty.name, COUNT(a) as appointmentCount " +
           "FROM Appointment a " +
           "WHERE a.dateTime >= :from AND a.dateTime <= :to " +
           "GROUP BY a.physician.physicianId, a.physician.fullName, a.physician.specialty.name " +
           "ORDER BY appointmentCount DESC")
    List<Object[]> findTop5PhysiciansByAppointmentCount(@Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to);

    Optional<Appointment> findByAppointmentId(String appointmentId);
}
