package leti_sisdis_6.hapappointmentrecords.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "saga_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SagaEvent {
    @Id
    private String id;
    private String appointmentId;
    private String type;          // e.g., APPOINTMENT_CREATED, RECORD_CREATED, COMPENSATION
    private String payloadJson;   // serialized JSON payload
    private String correlationId;
    private SagaStatus status;
    private Instant occurredAt;
}

