package leti_sisdis_6.happhysicians.dto.output;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;
import leti_sisdis_6.happhysicians.eventsourcing.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO para representar um evento no audit trail
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditTrailDTO {
    private Long eventId;
    private String aggregateId;
    private EventType eventType;
    private LocalDateTime timestamp;
    private Object eventData; // JSON object (parsed from string)
    private Long aggregateVersion;
    private String correlationId;
    private String userId;
    private Object metadata; // JSON object (parsed from string)
}

