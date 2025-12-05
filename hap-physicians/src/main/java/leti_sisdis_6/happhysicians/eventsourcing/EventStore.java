package leti_sisdis_6.happhysicians.eventsourcing;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Event Store - Armazenamento append-only de eventos para Event Sourcing
 * 
 * Este é um event store simples que armazena todos os eventos relacionados a consultas.
 * Os eventos são imutáveis e apenas adicionados (append-only), nunca modificados ou deletados.
 */
@Entity
@Table(name = "event_store", indexes = {
    @Index(name = "idx_aggregate_id", columnList = "aggregateId"),
    @Index(name = "idx_event_type", columnList = "eventType"),
    @Index(name = "idx_timestamp", columnList = "timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventStore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long eventId;

    /**
     * ID do agregado (appointmentId)
     */
    @Column(nullable = false, length = 10)
    private String aggregateId;

    /**
     * Tipo do evento (ConsultationScheduled, NoteAdded, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EventType eventType;

    /**
     * Timestamp do evento (quando ocorreu)
     */
    @Column(nullable = false)
    private LocalDateTime timestamp;

    /**
     * Dados do evento em formato JSON
     * Armazena todos os dados relevantes do evento
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String eventData;

    /**
     * Versão do agregado (para otimistic locking)
     */
    @Column(nullable = false)
    private Long aggregateVersion;

    /**
     * ID de correlação para rastreamento (opcional)
     */
    @Column(length = 100)
    private String correlationId;

    /**
     * Usuário que causou o evento (opcional)
     */
    @Column(length = 100)
    private String userId;

    /**
     * Metadados adicionais em formato JSON (opcional)
     */
    @Column(columnDefinition = "TEXT")
    private String metadata;
}

