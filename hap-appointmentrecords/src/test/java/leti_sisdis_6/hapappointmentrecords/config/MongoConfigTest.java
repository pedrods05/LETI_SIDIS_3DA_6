package leti_sisdis_6.hapappointmentrecords.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import static org.assertj.core.api.Assertions.assertThat;

class MongoConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Test
    @DisplayName("Should load MongoConfig when MongoDB URI is configured")
    void shouldLoadMongoConfigWhenUriIsConfigured() {
        contextRunner
                .withPropertyValues("spring.data.mongodb.uri=mongodb://localhost:27017/testdb")
                .withUserConfiguration(MongoConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(MongoConfig.class);
                });
    }

    @Test
    @DisplayName("Should not load MongoConfig when MongoDB URI is not configured")
    void shouldNotLoadMongoConfigWhenUriIsNotConfigured() {
        contextRunner
                .withUserConfiguration(MongoConfig.class)
                .run(context -> {
                    assertThat(context).doesNotHaveBean(MongoConfig.class);
                });
    }

    @Test
    @DisplayName("MongoConfig should have EnableMongoRepositories annotation")
    void mongoConfigShouldHaveEnableMongoRepositoriesAnnotation() {
        // When
        EnableMongoRepositories annotation = MongoConfig.class.getAnnotation(EnableMongoRepositories.class);

        // Then
        assertThat(annotation).isNotNull();
        assertThat(annotation.basePackages())
                .containsExactly("leti_sisdis_6.hapappointmentrecords.repository");
    }

    @Test
    @DisplayName("Should be conditional on MongoDB URI property")
    void shouldBeConditionalOnMongoUriProperty() {
        contextRunner
                .withPropertyValues("spring.data.mongodb.uri=")
                .withUserConfiguration(MongoConfig.class)
                .run(context -> {
                    // Empty URI should not load the config
                    assertThat(context).doesNotHaveBean(MongoConfig.class);
                });
    }

    @Test
    @DisplayName("Should load with valid MongoDB URI")
    void shouldLoadWithValidMongoUri() {
        contextRunner
                .withPropertyValues(
                        "spring.data.mongodb.uri=mongodb://user:password@localhost:27017/mydb?authSource=admin"
                )
                .withUserConfiguration(MongoConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(MongoConfig.class);
                });
    }

    @Test
    @DisplayName("Configuration class should be annotated with @Configuration")
    void configurationClassShouldHaveConfigurationAnnotation() {
        // When
        org.springframework.context.annotation.Configuration annotation =
                MongoConfig.class.getAnnotation(org.springframework.context.annotation.Configuration.class);

        // Then
        assertThat(annotation).isNotNull();
    }

    @Test
    @DisplayName("Should have ConditionalOnProperty annotation with correct parameters")
    void shouldHaveConditionalOnPropertyAnnotation() {
        // When
        org.springframework.boot.autoconfigure.condition.ConditionalOnProperty annotation =
                MongoConfig.class.getAnnotation(org.springframework.boot.autoconfigure.condition.ConditionalOnProperty.class);

        // Then
        assertThat(annotation).isNotNull();
        assertThat(annotation.prefix()).isEqualTo("spring.data.mongodb");
        assertThat(annotation.name()).containsExactly("uri");
    }
}

