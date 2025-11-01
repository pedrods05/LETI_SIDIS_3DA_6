package leti_sisdis_6.hapauth.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class JacksonConfigTest {

    private final JacksonConfig jacksonConfig = new JacksonConfig();

    @Test
    @DisplayName("ObjectMapper bean deve ser criado e configurar JavaTime corretamente")
    void objectMapper_shouldBeConfiguredForJavaTime() {
        ObjectMapper mapper = jacksonConfig.objectMapper();
        assertNotNull(mapper, "ObjectMapper não deve ser nulo");

        // Datas como strings, não timestamps
        assertFalse(mapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS),
                "WRITE_DATES_AS_TIMESTAMPS deve estar desativado");

        // Serialização básica de LocalDate/LocalDateTime sem lançar exceções
        assertDoesNotThrow(() -> mapper.writeValueAsString(LocalDate.of(2025, 1, 10)));
        assertDoesNotThrow(() -> mapper.writeValueAsString(LocalDateTime.of(2025, 1, 10, 10, 0, 0)));
    }

    @Test
    @DisplayName("ObjectMapper deve serializar LocalDate/LocalDateTime como texto (não numérico)")
    void objectMapper_shouldSerializeDatesAsText() throws JsonProcessingException {
        ObjectMapper mapper = jacksonConfig.objectMapper();

        String ld = mapper.writeValueAsString(LocalDate.of(2025, 1, 10));
        String ldt = mapper.writeValueAsString(LocalDateTime.of(2025, 1, 10, 10, 0, 0));

        // Verificações mínimas (formato textual, com ano e separadores)
        assertTrue(ld.startsWith("\"2025-"), "LocalDate deveria ser string tipo \"2025-01-10\"");
        assertTrue(ldt.startsWith("\"2025-"), "LocalDateTime deveria começar por \"2025-...\"");
        assertFalse(ld.matches("^\\d+$"), "LocalDate não deve ser timestamp numérico");
        assertFalse(ldt.matches("^\\d+$"), "LocalDateTime não deve ser timestamp numérico");
    }
}
