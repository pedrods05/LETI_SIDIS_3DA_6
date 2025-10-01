package leti_sisdis_6.hapclients;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class HapClientsApplicationTests {

    @Test
    void contextLoads() {
    }

}
