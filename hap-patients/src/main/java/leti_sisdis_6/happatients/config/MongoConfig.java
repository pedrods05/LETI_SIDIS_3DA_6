package leti_sisdis_6.happatients.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "leti_sisdis_6.happatients.query")
public class MongoConfig {
}