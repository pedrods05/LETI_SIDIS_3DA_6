package leti_sisdis_6.happatients.contract;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.junitsupport.target.HttpTestTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import leti_sisdis_6.happatients.repository.PatientRepository;
import leti_sisdis_6.happatients.model.Patient;
import java.util.Optional;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Provider("hap-patients-service")
@PactFolder("../hap-appointmentrecords/target/pacts")
public class PactProviderTest {

    @LocalServerPort
    private int port;

    @MockBean
    private PatientRepository patientRepository;

    @BeforeEach
    void setup(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("patient with ID 1 exists")
    public void patientExists() {
        Patient mockPatient = new Patient();
        mockPatient.setPatientId("1");
        mockPatient.setFullName("John Doe");
        mockPatient.setEmail("john@example.com");
        mockPatient.setPhoneNumber("123456789");
        mockPatient.setDataConsentGiven(true);
        when(patientRepository.findById("1")).thenReturn(Optional.of(mockPatient));
    }
}