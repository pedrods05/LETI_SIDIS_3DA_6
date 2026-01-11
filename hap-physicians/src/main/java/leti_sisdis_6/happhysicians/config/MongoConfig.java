package leti_sisdis_6.happhysicians.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "leti_sisdis_6.happhysicians.query")
public class MongoConfig {
}
