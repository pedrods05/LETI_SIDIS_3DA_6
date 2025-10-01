package leti_sisdis_6.hapclients;

import org.springframework.boot.SpringApplication;

public class TestHapClientsApplication {

    public static void main(String[] args) {
        SpringApplication.from(HapClientsApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
