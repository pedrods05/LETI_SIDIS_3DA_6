package leti_sisdis_6.hapappointmentrecords.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MongoConfigTest {
    @Test
    void testConfigExists() {
        MongoConfig config = new MongoConfig();
        assertNotNull(config);
    }
}