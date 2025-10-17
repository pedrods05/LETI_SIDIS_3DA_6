package leti_sisdis_6.hapappointmentrecords;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(exclude = {
        org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration.class,
        org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration.class
})
@ComponentScan(basePackages = {
        "leti_sisdis_6.hapappointmentrecords",
        "leti_sisdis_6.hapauth.api",
        "leti_sisdis_6.hapauth.usermanagement",
        "leti_sisdis_6.hapauth.services",
        "leti_sisdis_6.hapauth.configuration"
})
@EntityScan(basePackages = {
        "leti_sisdis_6.hapappointmentrecords.model",
        "leti_sisdis_6.happhysicians.model",
        "leti_sisdis_6.hapauth.model",
        "leti_sisdis_6.hapauth.usermanagement"
})
@EnableJpaRepositories(basePackages = {
        "leti_sisdis_6.hapappointmentrecords.repository",
        "leti_sisdis_6.happhysicians.repository",
        "leti_sisdis_6.hapauth.usermanagement"
})
public class HapAppointmentrecordsApplication {
    public static void main(String[] args) {
        SpringApplication.run(HapAppointmentrecordsApplication.class, args);
    }
}