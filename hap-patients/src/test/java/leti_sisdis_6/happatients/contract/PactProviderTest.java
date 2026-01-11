package leti_sisdis_6.happatients.contract;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import jakarta.servlet.*;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Collections;
import java.io.IOException;

// NOVOS IMPORTS NECESSÁRIOS
import org.springframework.data.mongodb.core.MongoTemplate;
import leti_sisdis_6.happatients.query.PatientQueryRepository;
import leti_sisdis_6.happatients.repository.PatientRepository;
import leti_sisdis_6.happatients.model.Patient;
import java.util.Optional;
import static org.mockito.Mockito.when;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "server.port=8080",
                "spring.profiles.active=test",
                "server.ssl.enabled=false",
                "spring.main.allow-bean-definition-overriding=true",
                // Mantemos a exclusão para não haver tentativas reais de conexão
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration"
        }
)
@Provider("hap-patients-service")
@PactFolder("../hap-appointmentrecords/target/pacts")
public class PactProviderTest {

    // --- MOCKS DE INFRAESTRUTURA ---

    // 1. O Mock principal do repositório JPA/Mongo que usamos no teste
    @MockBean
    private PatientRepository patientRepository;

    // 2. [NOVO] Mockar o MongoTemplate para satisfazer o @EnableMongoRepositories
    // Isto evita o erro "No bean named 'mongoTemplate'" sem ligar à BD real
    @MockBean
    private MongoTemplate mongoTemplate;

    // 3. [NOVO] Mockar o repositório de Query que estava a dar erro ao iniciar
    @MockBean
    private PatientQueryRepository patientQueryRepository;

    // -------------------------------

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .addFilterBefore(new Filter() {
                        @Override
                        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                                throws IOException, ServletException {
                            SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ADMIN");
                            UsernamePasswordAuthenticationToken auth =
                                    new UsernamePasswordAuthenticationToken("pact-test-admin", null, Collections.singletonList(authority));
                            SecurityContextHolder.getContext().setAuthentication(auth);
                            chain.doFilter(request, response);
                        }
                    }, AnonymousAuthenticationFilter.class);

            return http.build();
        }
    }

    @BeforeEach
    void setup(PactVerificationContext context) {
        System.setProperty("pact.verifier.publishResults", "false");
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

        mockPatient.setPhoneNumber("912345678");
        mockPatient.setDataConsentGiven(true);
        mockPatient.setBirthDate(java.time.LocalDate.of(1990, 1, 1));

        when(patientRepository.findById("1")).thenReturn(Optional.of(mockPatient));
    }
}