package leti_sisdis_6.happatients.config; // Ajusta o pacote se necessário

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
// A anotação vem para aqui. Confirma se o pacote "basePackages" está correto!
@EnableMongoRepositories(basePackages = "leti_sisdis_6.happatients.query")
public class MongoConfig {
    // Fica vazia. Serve só para carregar o Mongo quando a app arranca normalmente.
}
