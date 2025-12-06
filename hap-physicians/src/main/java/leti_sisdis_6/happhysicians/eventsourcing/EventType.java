package leti_sisdis_6.happhysicians.eventsourcing;

/**
 * Tipos de eventos para Event Sourcing
 * Representa todos os eventos que podem ocorrer em uma consulta
 */
public enum EventType {
    /**
     * Consulta agendada
     */
    CONSULTATION_SCHEDULED("ConsultationScheduled"),
    
    /**
     * Consulta atualizada (mudança de data/hora, tipo, etc.)
     */
    CONSULTATION_UPDATED("ConsultationUpdated"),
    
    /**
     * Nota adicionada à consulta
     */
    NOTE_ADDED("NoteAdded"),
    
    /**
     * Consulta cancelada
     */
    CONSULTATION_CANCELED("ConsultationCanceled"),
    
    /**
     * Consulta concluída
     */
    CONSULTATION_COMPLETED("ConsultationCompleted"),
    
    /**
     * Consulta reagendada
     */
    CONSULTATION_RESCHEDULED("ConsultationRescheduled");

    private final String value;

    EventType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static EventType fromString(String text) {
        for (EventType type : EventType.values()) {
            if (type.value.equalsIgnoreCase(text)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid event type: " + text);
    }
}

