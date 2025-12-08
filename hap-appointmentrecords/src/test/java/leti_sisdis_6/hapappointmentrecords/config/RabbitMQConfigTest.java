package leti_sisdis_6.hapappointmentrecords.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Collection;

import static leti_sisdis_6.hapappointmentrecords.config.RabbitMQConfig.CORRELATION_ID_HEADER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class RabbitMQConfigTest {

    @Test
    @DisplayName("RabbitTemplate deve ter PostProcessor que injeta CorrelationId")
    void rabbitTemplate_PostProcessorLogic() {
        RabbitMQConfig config = new RabbitMQConfig();
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        MDC.put(CORRELATION_ID_HEADER, "mq-correlation-123");

        RabbitTemplate template = config.rabbitTemplate(connectionFactory);


        Collection<MessagePostProcessor> processors = template.getBeforePublishPostProcessors();
        MessagePostProcessor processor = processors.iterator().next();

        Message message = new Message(new byte[0], new MessageProperties());

        Message resultMsg = processor.postProcessMessage(message);

        assertEquals("mq-correlation-123", resultMsg.getMessageProperties().getHeader(CORRELATION_ID_HEADER));

        MDC.clear();
    }
}