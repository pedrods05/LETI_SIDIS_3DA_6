package leti_sisdis_6.hapappointmentrecords.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import leti_sisdis_6.hapappointmentrecords.model.AppointmentRecordProjection;

public interface AppointmentRecordProjectionRepository extends MongoRepository<AppointmentRecordProjection, String> {
    List<AppointmentRecordProjection> findByPatientId(String patientId);
}

