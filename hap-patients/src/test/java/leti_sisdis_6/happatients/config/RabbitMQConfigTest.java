package leti_sisdis_6.happatients.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = RabbitMQConfig.class)
@TestPropertySource(properties = {
        "hap.rabbitmq.exchange=hap-exchange-test"
})
class RabbitMQConfigTest {

    @MockBean
    private ConnectionFactory connectionFactory;

    @Test
    void correlationIdHeader_shouldBeCorrectlyDefined() {
        // Assert
        assertThat(RabbitMQConfig.CORRELATION_ID_HEADER).isEqualTo("X-Correlation-Id");
    }

    @Test
    void exchange_shouldCreateTopicExchange() {
        // Arrange
        RabbitMQConfig config = new RabbitMQConfig();

        // Act
        TopicExchange exchange = config.exchange();

        // Assert
        assertThat(exchange).isNotNull();
        assertThat(exchange.getType()).isEqualTo("topic");
    }

    @Test
    void producerMessageConverter_shouldCreateJacksonConverter() {
        // Arrange
        RabbitMQConfig config = new RabbitMQConfig();

        // Act
        Jackson2JsonMessageConverter converter = config.producerMessageConverter();

        // Assert
        assertThat(converter).isNotNull();
    }

    @Test
    void rabbitTemplate_shouldBeConfiguredWithMessageConverter() {
        // Arrange
        RabbitMQConfig config = new RabbitMQConfig();

        // Act
        RabbitTemplate rabbitTemplate = config.rabbitTemplate(connectionFactory);

        // Assert
        assertThat(rabbitTemplate).isNotNull();
        assertThat(rabbitTemplate.getMessageConverter()).isInstanceOf(Jackson2JsonMessageConverter.class);
    }

    @Test
    void rabbitTemplate_shouldBeCreatedSuccessfully() {
        // Arrange
        RabbitMQConfig config = new RabbitMQConfig();

        // Act
        RabbitTemplate rabbitTemplate = config.rabbitTemplate(connectionFactory);

        // Assert
        assertThat(rabbitTemplate).isNotNull();
    }
}
