package leti_sisdis_6.hapappointmentrecords;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = HapAppointmentrecordsApplication.class
)
@ActiveProfiles("test")
class HapAppointmentrecordsApplicationTests {

    @Test
    @DisplayName("Deve carregar contexto da aplicação")
    void contextLoads() {
    }

}
