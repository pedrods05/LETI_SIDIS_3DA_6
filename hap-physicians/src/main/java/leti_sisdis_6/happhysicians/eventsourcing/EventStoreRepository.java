package leti_sisdis_6.happhysicians.eventsourcing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository para Event Store (append-only)
 */
@Repository
public interface EventStoreRepository extends JpaRepository<EventStore, Long> {

    /**
     * Busca todos os eventos de um agregado (appointment) ordenados por timestamp
     */
    List<EventStore> findByAggregateIdOrderByTimestampAsc(String aggregateId);

    /**
     * Busca eventos por tipo
     */
    List<EventStore> findByEventTypeOrderByTimestampDesc(EventType eventType);

    /**
     * Busca eventos de um agregado por tipo
     */
    List<EventStore> findByAggregateIdAndEventTypeOrderByTimestampAsc(
            String aggregateId, 
            EventType eventType
    );

    /**
     * Busca eventos em um intervalo de tempo
     */
    @Query("SELECT e FROM EventStore e WHERE e.timestamp BETWEEN :start AND :end ORDER BY e.timestamp DESC")
    List<EventStore> findEventsBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    /**
     * Busca a última versão do agregado
     */
    @Query("SELECT COALESCE(MAX(e.aggregateVersion), 0) FROM EventStore e WHERE e.aggregateId = :aggregateId")
    Long findLastVersionByAggregateId(@Param("aggregateId") String aggregateId);

    /**
     * Conta eventos de um agregado
     */
    long countByAggregateId(String aggregateId);
}

