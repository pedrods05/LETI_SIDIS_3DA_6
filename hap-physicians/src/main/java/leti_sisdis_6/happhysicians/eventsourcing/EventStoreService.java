package leti_sisdis_6.happhysicians.eventsourcing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Serviço para gerenciar Event Store (append-only)
 * 
 * Responsável por salvar eventos no event store e recuperar histórico
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventStoreService {

    private final EventStoreRepository eventStoreRepository;
    private final ObjectMapper objectMapper;

    /**
     * Salva um evento no event store (append-only)
     * 
     * @param aggregateId ID do agregado (appointmentId)
     * @param eventType Tipo do evento
     * @param eventData Dados do evento (será serializado para JSON)
     * @param correlationId ID de correlação (opcional)
     * @param userId ID do usuário que causou o evento (opcional)
     * @param metadata Metadados adicionais (opcional)
     * @return Evento salvo
     */
    @Transactional
    public EventStore saveEvent(
            String aggregateId,
            EventType eventType,
            Object eventData,
            String correlationId,
            String userId,
            Map<String, Object> metadata
    ) {
        try {
            // Buscar próxima versão do agregado
            Long currentVersion = eventStoreRepository.findLastVersionByAggregateId(aggregateId);
            Long nextVersion = currentVersion + 1;

            // Serializar dados do evento para JSON
            String eventDataJson = objectMapper.writeValueAsString(eventData);
            String metadataJson = metadata != null ? objectMapper.writeValueAsString(metadata) : null;

            // Criar e salvar evento
            EventStore event = EventStore.builder()
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .timestamp(LocalDateTime.now())
                    .eventData(eventDataJson)
                    .aggregateVersion(nextVersion)
                    .correlationId(correlationId)
                    .userId(userId)
                    .metadata(metadataJson)
                    .build();

            EventStore saved = eventStoreRepository.save(event);
            log.info("✅ Event saved to Event Store: {} - {} (version: {})", 
                    aggregateId, eventType.getValue(), nextVersion);
            
            return saved;
        } catch (JsonProcessingException e) {
            log.error("❌ Failed to serialize event data for aggregate: {}", aggregateId, e);
            throw new RuntimeException("Failed to save event to event store", e);
        }
    }

    /**
     * Busca todos os eventos de um agregado (audit trail)
     * 
     * @param aggregateId ID do agregado
     * @return Lista de eventos ordenados por timestamp
     */
    public List<EventStore> getEventHistory(String aggregateId) {
        return eventStoreRepository.findByAggregateIdOrderByTimestampAsc(aggregateId);
    }

    /**
     * Busca eventos por tipo
     * 
     * @param eventType Tipo do evento
     * @return Lista de eventos
     */
    public List<EventStore> getEventsByType(EventType eventType) {
        return eventStoreRepository.findByEventTypeOrderByTimestampDesc(eventType);
    }

    /**
     * Busca eventos de um agregado por tipo
     * 
     * @param aggregateId ID do agregado
     * @param eventType Tipo do evento
     * @return Lista de eventos
     */
    public List<EventStore> getEventsByAggregateAndType(String aggregateId, EventType eventType) {
        return eventStoreRepository.findByAggregateIdAndEventTypeOrderByTimestampAsc(aggregateId, eventType);
    }

    /**
     * Busca eventos em um intervalo de tempo
     * 
     * @param start Data/hora inicial
     * @param end Data/hora final
     * @return Lista de eventos
     */
    public List<EventStore> getEventsBetween(LocalDateTime start, LocalDateTime end) {
        return eventStoreRepository.findEventsBetween(start, end);
    }

    /**
     * Reconstroi o estado atual de um agregado a partir dos eventos
     * 
     * @param aggregateId ID do agregado
     * @return Último evento (representa o estado atual)
     */
    public EventStore getCurrentState(String aggregateId) {
        List<EventStore> events = getEventHistory(aggregateId);
        if (events.isEmpty()) {
            return null;
        }
        // Retorna o último evento (maior versão)
        return events.get(events.size() - 1);
    }
}

