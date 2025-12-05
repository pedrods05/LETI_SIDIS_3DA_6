package leti_sisdis_6.hapappointmentrecords.repository;

import leti_sisdis_6.hapappointmentrecords.model.AppointmentProjection;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentProjectionRepository extends MongoRepository<AppointmentProjection, String> {
    List<AppointmentProjection> findByPatientId(String patientId);
    List<AppointmentProjection> findByPhysicianId(String physicianId);
    List<AppointmentProjection> findByPhysicianIdAndDateTime(String physicianId, LocalDateTime dateTime);
}
