package leti_sisdis_6.hapauth.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(JacksonConfig.class)
class JacksonConfigTest {

    @Autowired
    private ObjectMapper mapper;   // ← injeta aqui

    @Test
    @DisplayName("ObjectMapper deve estar configurado para JavaTime e sem timestamps")
    void objectMapper_shouldHandleJavaTime() throws JsonProcessingException {
        assertNotNull(mapper);

        assertFalse(mapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));

        String ld = mapper.writeValueAsString(LocalDate.of(2025, 1, 10));
        String ldt = mapper.writeValueAsString(LocalDateTime.of(2025, 1, 10, 10, 0));

        assertTrue(ld.startsWith("\"2025-"));
        assertTrue(ldt.startsWith("\"2025-"));
        assertFalse(ld.matches("^\\d+$"));
        assertFalse(ldt.matches("^\\d+$"));
    }
}
