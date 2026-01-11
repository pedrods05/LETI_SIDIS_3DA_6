package leti_sisdis_6.happhysicians;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import jakarta.annotation.PostConstruct;

@SpringBootApplication(scanBasePackages = {
        "leti_sisdis_6.happhysicians"
})
@EnableJpaRepositories(basePackages = {
        "leti_sisdis_6.happhysicians.repository",
        "leti_sisdis_6.happhysicians.eventsourcing"
})
//@EnableMongoRepositories(basePackages = {
//        "leti_sisdis_6.happhysicians.query"
//})
@EnableRabbit
@EntityScan(basePackages = {
        "leti_sisdis_6.happhysicians.model",
        "leti_sisdis_6.happhysicians.eventsourcing"
})
@Slf4j
public class HapPhysiciansApplication {

    private final Environment environment;

    @Value("${server.port:8081}")
    private String serverPort;

    public HapPhysiciansApplication(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void logConfiguration() {
        String[] activeProfiles = environment.getActiveProfiles();
        String mongoUri = environment.getProperty("spring.data.mongodb.uri", "NOT_SET");
        
        log.info("═══════════════════════════════════════════════════════════");
        log.info("🚀 HAP Physicians Application Starting");
        log.info("📌 Server Port: {}", serverPort);
        log.info("📌 Active Profiles: {}", String.join(", ", activeProfiles));
        log.info("📌 MongoDB URI: {}", mongoUri);
        log.info("═══════════════════════════════════════════════════════════");
    }

    public static void main(String[] args) {
        SpringApplication.run(HapPhysiciansApplication.class, args);
    }
}
