package leti_sisdis_6.happatients.event;

import leti_sisdis_6.happatients.query.PatientQueryRepository;
import leti_sisdis_6.happatients.query.PatientSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static leti_sisdis_6.happatients.config.RabbitMQConfig.CORRELATION_ID_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientEventHandlerTest {

    @Mock
    private PatientQueryRepository queryRepository;

    @InjectMocks
    private PatientEventHandler eventHandler;

    private PatientRegisteredEvent testEvent;
    private Message testMessage;

    @BeforeEach
    void setUp() {
        PatientRegisteredEvent.AddressEventData address = new PatientRegisteredEvent.AddressEventData(
                "Rua das Flores 123",
                "Porto",
                "4000-123",
                "Portugal"
        );

        PatientRegisteredEvent.InsuranceEventData insurance = new PatientRegisteredEvent.InsuranceEventData(
                "POL123456",
                "Seguradora XYZ",
                "Premium"
        );

        testEvent = new PatientRegisteredEvent(
                "PAT01",
                "João Silva",
                "joao.silva@example.com",
                "912345678",
                LocalDate.of(1990, 5, 15),
                true,
                LocalDate.now(),
                address,
                insurance
        );

        MessageProperties properties = new MessageProperties();
        Map<String, Object> headers = new HashMap<>();
        headers.put(CORRELATION_ID_HEADER, "test-correlation-id-123");
        properties.getHeaders().putAll(headers);
        testMessage = new Message("test".getBytes(), properties);
    }

    @Test
    void handlePatientRegistered_shouldSavePatientSummaryToMongo() {
        // Arrange
        when(queryRepository.save(any(PatientSummary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        eventHandler.handlePatientRegistered(testEvent, testMessage, "test-correlation-id-123");

        // Assert
        ArgumentCaptor<PatientSummary> summaryCaptor = ArgumentCaptor.forClass(PatientSummary.class);
        verify(queryRepository, times(1)).save(summaryCaptor.capture());

        PatientSummary savedSummary = summaryCaptor.getValue();
        assertThat(savedSummary.getPatientId()).isEqualTo("PAT01");
        assertThat(savedSummary.getFullName()).isEqualTo("João Silva");
        assertThat(savedSummary.getEmail()).isEqualTo("joao.silva@example.com");
        assertThat(savedSummary.getPhoneNumber()).isEqualTo("912345678");
        assertThat(savedSummary.getBirthDate()).isEqualTo(LocalDate.of(1990, 5, 15));
        assertThat(savedSummary.getDataConsentGiven()).isTrue();

        // Verify address mapping
        assertThat(savedSummary.getAddress().getStreet()).isEqualTo("Rua das Flores 123");
        assertThat(savedSummary.getAddress().getCity()).isEqualTo("Porto");
        assertThat(savedSummary.getAddress().getPostalCode()).isEqualTo("4000-123");
        assertThat(savedSummary.getAddress().getCountry()).isEqualTo("Portugal");

        // Verify insurance mapping
        assertThat(savedSummary.getInsuranceInfo().getPolicyNumber()).isEqualTo("POL123456");
        assertThat(savedSummary.getInsuranceInfo().getProvider()).isEqualTo("Seguradora XYZ");
        assertThat(savedSummary.getInsuranceInfo().getCoverageType()).isEqualTo("Premium");
    }

    @Test
    void handlePatientRegistered_shouldExtractCorrelationIdFromMessageHeaders() {
        // Arrange
        when(queryRepository.save(any(PatientSummary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        eventHandler.handlePatientRegistered(testEvent, testMessage, null);

        // Assert
        verify(queryRepository, times(1)).save(any(PatientSummary.class));
    }

    @Test
    void handlePatientRegistered_shouldHandleNullCorrelationId() {
        // Arrange
        MessageProperties properties = new MessageProperties();
        Message messageWithoutCorrelation = new Message("test".getBytes(), properties);
        when(queryRepository.save(any(PatientSummary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        eventHandler.handlePatientRegistered(testEvent, messageWithoutCorrelation, null);

        // Assert
        verify(queryRepository, times(1)).save(any(PatientSummary.class));
    }
}

