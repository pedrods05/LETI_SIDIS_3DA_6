package leti_sisdis_6.hapappointmentrecords;

import org.springframework.boot.SpringApplication;

public class TestHapAppointmentrecordsApplication {

    public static void main(String[] args) {
        SpringApplication.from(HapAppointmentrecordsApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
