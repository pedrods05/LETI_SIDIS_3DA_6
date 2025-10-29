package leti_sisdis_6.hapappointmentrecords;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
        scanBasePackages = {
                "leti_sisdis_6.hapappointmentrecords",
        }
)
public class HapAppointmentrecordsApplication {
    public static void main(String[] args) {
        SpringApplication.run(HapAppointmentrecordsApplication.class, args);
    }
}