package leti_sisdis_6.happhysicians.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConsultationTypeTest {

    @Test
    void testEnumValues() {
        // Verifica que todos os valores do enum existem
        ConsultationType[] values = ConsultationType.values();
        assertEquals(2, values.length);
        assertTrue(containsValue(values, ConsultationType.FIRST_TIME));
        assertTrue(containsValue(values, ConsultationType.FOLLOW_UP));
    }

    @Test
    void testGetValue() {
        assertEquals("First-time", ConsultationType.FIRST_TIME.getValue());
        assertEquals("Follow-up", ConsultationType.FOLLOW_UP.getValue());
    }

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

    private boolean containsValue(ConsultationType[] values, ConsultationType type) {
        for (ConsultationType value : values) {
            if (value == type) {
                return true;
            }
        }
        return false;
    }
}

