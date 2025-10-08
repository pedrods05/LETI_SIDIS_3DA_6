package leti_sisdis_6.happatients;

import org.springframework.boot.SpringApplication;

public class TestHapPatientsApplication {

    public static void main(String[] args) {
        SpringApplication.from(HapPatientsApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
