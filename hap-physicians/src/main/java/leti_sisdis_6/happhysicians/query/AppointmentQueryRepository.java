package leti_sisdis_6.happhysicians.query;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentQueryRepository extends MongoRepository<AppointmentSummary, String> {
    List<AppointmentSummary> findByDateTimeAfterOrderByDateTimeAsc(LocalDateTime dateTime);
    List<AppointmentSummary> findByStatus(String status);
    List<AppointmentSummary> findByPhysicianId(String physicianId);
    List<AppointmentSummary> findByPatientId(String patientId);
}

