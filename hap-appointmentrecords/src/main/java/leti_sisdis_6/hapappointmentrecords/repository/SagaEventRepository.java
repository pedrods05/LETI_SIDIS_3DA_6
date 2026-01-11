package leti_sisdis_6.hapappointmentrecords.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import leti_sisdis_6.hapappointmentrecords.model.SagaEvent;
import leti_sisdis_6.hapappointmentrecords.model.SagaStatus;

public interface SagaEventRepository extends MongoRepository<SagaEvent, String> {
    List<SagaEvent> findByAppointmentIdOrderByOccurredAtAsc(String appointmentId);
    List<SagaEvent> findByAppointmentIdAndStatusOrderByOccurredAtAsc(String appointmentId, SagaStatus status);
}

