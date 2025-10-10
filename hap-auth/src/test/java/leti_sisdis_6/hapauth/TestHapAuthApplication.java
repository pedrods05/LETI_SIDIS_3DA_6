package leti_sisdis_6.hapauth;

import org.springframework.boot.SpringApplication;

public class TestHapAuthApplication {

    public static void main(String[] args) {
        SpringApplication.from(HapAuthApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
