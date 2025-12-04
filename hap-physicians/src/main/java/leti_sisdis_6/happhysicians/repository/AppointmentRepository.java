package leti_sisdis_6.happhysicians.repository;

import leti_sisdis_6.happhysicians.model.Appointment;
import leti_sisdis_6.happhysicians.model.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, String> {
    boolean existsByPhysicianPhysicianIdAndDateTime(String physicianId, LocalDateTime dateTime);

    List<Appointment> findByPatientId(String patientId);

    List<Appointment> findByPatientIdOrderByDateTimeDesc(String patientId);

    List<Appointment> findByDateTimeAfterOrderByDateTimeAsc(LocalDateTime dateTime);

    List<Appointment> findByPhysicianPhysicianIdAndDateTimeBetween(String physicianId, LocalDateTime start, LocalDateTime end);

    List<Appointment> findByDateTimeBetween(LocalDateTime start, LocalDateTime end);

    List<Appointment> findByPatientIdAndStatus(String patientId, AppointmentStatus status);

    List<Appointment> findByPhysicianPhysicianId(String physicianId);

    List<Appointment> findByDateTimeAfterAndStatusOrderByDateTimeAsc(LocalDateTime dateTime, AppointmentStatus status);

    // Removed query that depended on AppointmentRecord

    @Query("SELECT a.physician.physicianId, a.physician.fullName, a.physician.specialty.name, COUNT(a) as appointmentCount " +
            "FROM Appointment a " +
            "WHERE a.dateTime >= :from AND a.dateTime <= :to " +
            "GROUP BY a.physician.physicianId, a.physician.fullName, a.physician.specialty.name " +
            "ORDER BY appointmentCount DESC")
    List<Object[]> findTop5PhysiciansByAppointmentCount(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
