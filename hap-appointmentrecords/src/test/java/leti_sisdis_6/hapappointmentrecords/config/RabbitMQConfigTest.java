package leti_sisdis_6.hapappointmentrecords.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;

import static leti_sisdis_6.hapappointmentrecords.config.RabbitMQConfig.CORRELATION_ID_HEADER;
import static leti_sisdis_6.hapappointmentrecords.config.RabbitMQConfig.DEFAULT_EXCHANGE;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RabbitMQConfigTest {

    private RabbitMQConfig config;

    @Mock
    private ConnectionFactory connectionFactory;

    @BeforeEach
    void setUp() {
        config = new RabbitMQConfig();
        MDC.clear();
    }

    @Test
    @DisplayName("Should create TopicExchange with default name when property not set")
    void shouldCreateTopicExchangeWithDefaultName() {
        // When
        TopicExchange exchange = config.exchange();

        // Then
        assertThat(exchange).isNotNull();
        assertThat(exchange.getName()).isEqualTo(DEFAULT_EXCHANGE);
        assertThat(exchange.getType()).isEqualTo("topic");
    }

    @Test
    @DisplayName("Should create TopicExchange with configured name")
    void shouldCreateTopicExchangeWithConfiguredName() {
        // Given
        String customExchange = "custom-exchange";
        ReflectionTestUtils.setField(config, "exchangeName", customExchange);

        // When
        TopicExchange exchange = config.exchange();

        // Then
        assertThat(exchange).isNotNull();
        assertThat(exchange.getName()).isEqualTo(customExchange);
    }

    @Test
    @DisplayName("Should create RabbitAdmin with auto startup enabled")
    void shouldCreateRabbitAdminWithAutoStartup() {
        // When
        RabbitAdmin rabbitAdmin = config.rabbitAdmin(connectionFactory);

        // Then
        assertThat(rabbitAdmin).isNotNull();
    }

    @Test
    @DisplayName("Should create Jackson2JsonMessageConverter")
    void shouldCreateJackson2JsonMessageConverter() {
        // When
        Jackson2JsonMessageConverter converter = config.producerMessageConverter();

        // Then
        assertThat(converter).isNotNull();
    }

    @Test
    @DisplayName("Should create RabbitTemplate with message converter")
    void shouldCreateRabbitTemplateWithMessageConverter() {
        // When
        RabbitTemplate rabbitTemplate = config.rabbitTemplate(connectionFactory);

        // Then
        assertThat(rabbitTemplate).isNotNull();
        assertThat(rabbitTemplate.getMessageConverter()).isInstanceOf(Jackson2JsonMessageConverter.class);
    }

    @Test
    @DisplayName("RabbitTemplate should add correlation ID from MDC to message headers")
    void rabbitTemplateShouldAddCorrelationIdFromMDC() {
        // Given
        String correlationId = "test-correlation-123";
        MDC.put(CORRELATION_ID_HEADER, correlationId);

        RabbitTemplate rabbitTemplate = config.rabbitTemplate(connectionFactory);

        // Create a test message
        MessageProperties messageProperties = new MessageProperties();
        Message message = new Message("test".getBytes(), messageProperties);

        // Get the post processor
        MessagePostProcessor[] postProcessors =
                (MessagePostProcessor[]) ReflectionTestUtils.getField(rabbitTemplate, "beforePublishPostProcessors");

        // When
        assertThat(postProcessors).isNotNull();
        Message processedMessage = postProcessors[0].postProcessMessage(message);

        // Then
        Object headerValue = processedMessage.getMessageProperties().getHeader(CORRELATION_ID_HEADER);
        assertThat(headerValue).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("RabbitTemplate should not add correlation ID when not in MDC")
    void rabbitTemplateShouldNotAddCorrelationIdWhenNotInMDC() {
        // Given
        MDC.clear();

        RabbitTemplate rabbitTemplate = config.rabbitTemplate(connectionFactory);

        // Create a test message
        MessageProperties messageProperties = new MessageProperties();
        Message message = new Message("test".getBytes(), messageProperties);

        // Get the post processor
        MessagePostProcessor[] postProcessors =
                (MessagePostProcessor[]) ReflectionTestUtils.getField(rabbitTemplate, "beforePublishPostProcessors");

        // When
        assertThat(postProcessors).isNotNull();
        Message processedMessage = postProcessors[0].postProcessMessage(message);

        // Then
        Object headerValue = processedMessage.getMessageProperties().getHeader(CORRELATION_ID_HEADER);
        assertThat(headerValue).isNull();
    }

    @Test
    @DisplayName("RabbitTemplate should not add correlation ID when blank in MDC")
    void rabbitTemplateShouldNotAddCorrelationIdWhenBlankInMDC() {
        // Given
        MDC.put(CORRELATION_ID_HEADER, "   ");

        RabbitTemplate rabbitTemplate = config.rabbitTemplate(connectionFactory);

        // Create a test message
        MessageProperties messageProperties = new MessageProperties();
        Message message = new Message("test".getBytes(), messageProperties);

        // Get the post processor
        MessagePostProcessor[] postProcessors =
                (MessagePostProcessor[]) ReflectionTestUtils.getField(rabbitTemplate, "beforePublishPostProcessors");

        // When
        assertThat(postProcessors).isNotNull();
        Message processedMessage = postProcessors[0].postProcessMessage(message);

        // Then
        Object headerValue = processedMessage.getMessageProperties().getHeader(CORRELATION_ID_HEADER);
        assertThat(headerValue).isNull();
    }

    @Test
    @DisplayName("Should have correct correlation ID header constant")
    void shouldHaveCorrectCorrelationIdHeaderConstant() {
        assertThat(CORRELATION_ID_HEADER).isEqualTo("X-Correlation-Id");
    }

    @Test
    @DisplayName("Should have correct default exchange constant")
    void shouldHaveCorrectDefaultExchangeConstant() {
        assertThat(DEFAULT_EXCHANGE).isEqualTo("hap-exchange");
    }

    @Test
    @DisplayName("RabbitTemplate post processor should preserve existing message properties")
    void postProcessorShouldPreserveExistingProperties() {
        // Given
        String correlationId = "test-correlation-123";
        MDC.put(CORRELATION_ID_HEADER, correlationId);

        RabbitTemplate rabbitTemplate = config.rabbitTemplate(connectionFactory);

        // Create a test message with existing properties
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType("application/json");
        messageProperties.setHeader("Custom-Header", "custom-value");
        Message message = new Message("test".getBytes(), messageProperties);

        // Get the post processor
        MessagePostProcessor[] postProcessors =
                (MessagePostProcessor[]) ReflectionTestUtils.getField(rabbitTemplate, "beforePublishPostProcessors");

        // When
        assertThat(postProcessors).isNotNull();
        Message processedMessage = postProcessors[0].postProcessMessage(message);

        // Then
        assertThat(processedMessage.getMessageProperties().getContentType())
                .isEqualTo("application/json");
        Object customHeader = processedMessage.getMessageProperties().getHeader("Custom-Header");
        assertThat(customHeader).isEqualTo("custom-value");
        Object correlationHeader = processedMessage.getMessageProperties().getHeader(CORRELATION_ID_HEADER);
        assertThat(correlationHeader).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("Configuration class should be annotated with @Configuration")
    void configurationClassShouldHaveConfigurationAnnotation() {
        // When
        org.springframework.context.annotation.Configuration annotation =
                RabbitMQConfig.class.getAnnotation(org.springframework.context.annotation.Configuration.class);

        // Then
        assertThat(annotation).isNotNull();
    }

    @Test
    @DisplayName("Exchange method should be annotated with @Bean")
    void exchangeMethodShouldHaveBeanAnnotation() throws NoSuchMethodException {
        // When
        org.springframework.context.annotation.Bean annotation =
                RabbitMQConfig.class.getMethod("exchange").getAnnotation(org.springframework.context.annotation.Bean.class);

        // Then
        assertThat(annotation).isNotNull();
    }

    @Test
    @DisplayName("RabbitAdmin method should be annotated with @Bean")
    void rabbitAdminMethodShouldHaveBeanAnnotation() throws NoSuchMethodException {
        // When
        org.springframework.context.annotation.Bean annotation =
                RabbitMQConfig.class.getMethod("rabbitAdmin", ConnectionFactory.class)
                        .getAnnotation(org.springframework.context.annotation.Bean.class);

        // Then
        assertThat(annotation).isNotNull();
    }

    @Test
    @DisplayName("RabbitTemplate method should be annotated with @Bean")
    void rabbitTemplateMethodShouldHaveBeanAnnotation() throws NoSuchMethodException {
        // When
        org.springframework.context.annotation.Bean annotation =
                RabbitMQConfig.class.getMethod("rabbitTemplate", ConnectionFactory.class)
                        .getAnnotation(org.springframework.context.annotation.Bean.class);

        // Then
        assertThat(annotation).isNotNull();
    }
}

