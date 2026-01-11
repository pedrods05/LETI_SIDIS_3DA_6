package leti_sisdis_6.hapappointmentrecords.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import leti_sisdis_6.hapappointmentrecords.model.EventStoreEntry;

public interface EventStoreRepository extends MongoRepository<EventStoreEntry, String> {
    List<EventStoreEntry> findByAppointmentIdOrderByOccurredAtAsc(String appointmentId);
}

