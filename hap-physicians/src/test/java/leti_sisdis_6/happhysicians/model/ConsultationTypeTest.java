package leti_sisdis_6.happhysicians.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConsultationTypeTest {

    @Test
    void testFromString_ValidValues() {
        assertEquals(ConsultationType.FIRST_TIME, ConsultationType.fromString("First-time"));
        assertEquals(ConsultationType.FOLLOW_UP, ConsultationType.fromString("Follow-up"));
        
        // Case insensitive
        assertEquals(ConsultationType.FIRST_TIME, ConsultationType.fromString("first-time"));
        assertEquals(ConsultationType.FIRST_TIME, ConsultationType.fromString("FIRST-TIME"));
        assertEquals(ConsultationType.FOLLOW_UP, ConsultationType.fromString("follow-up"));
        assertEquals(ConsultationType.FOLLOW_UP, ConsultationType.fromString("FOLLOW-UP"));
    }

    @Test
    void testFromString_InvalidValue() {
        assertThrows(IllegalArgumentException.class, () -> {
            ConsultationType.fromString("INVALID_TYPE");
        });
    }

    @Test
    void testFromString_Null() {
        assertThrows(IllegalArgumentException.class, () -> {
            ConsultationType.fromString(null);
        });
    }

    @Test
    void testFromString_EmptyString() {
        assertThrows(IllegalArgumentException.class, () -> {
            ConsultationType.fromString("");
        });
    }
}

