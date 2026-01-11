package leti_sisdis_6.hapappointmentrecords.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "appointment_event_store")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventStoreEntry {
    @Id
    private String id;
    private String appointmentId;
    private String eventType;
    private String payload;
    private String correlationId;
    private boolean compensation;
    private Instant occurredAt;
}

