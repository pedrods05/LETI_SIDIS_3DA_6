package leti_sisdis_6.happatients;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test") // <--- ADICIONA ISTO para carregar o application-test.properties
class HapPatientsApplicationTests {

    @Test
    void contextLoads() {
    }

}
