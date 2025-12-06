package leti_sisdis_6.hapappointmentrecords.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "leti_sisdis_6.hapappointmentrecords.repository")
@ConditionalOnProperty(prefix = "spring.data.mongodb", name = "uri")
public class MongoConfig {
}

