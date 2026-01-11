package leti_sisdis_6.happhysicians.contract;

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
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.TestPropertySource;
import org.springframework.boot.test.mock.mockito.MockBean;
import leti_sisdis_6.happhysicians.config.SecurityConfig;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import leti_sisdis_6.happhysicians.query.PhysicianQueryService;
import leti_sisdis_6.happhysicians.query.AppointmentQueryService;
import jakarta.servlet.*;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.mongodb.core.MongoTemplate;
import leti_sisdis_6.happhysicians.query.PhysicianQueryRepository;
import leti_sisdis_6.happhysicians.query.AppointmentQueryRepository;
import leti_sisdis_6.happhysicians.repository.PhysicianRepository;
import leti_sisdis_6.happhysicians.repository.AppointmentRepository;
import leti_sisdis_6.happhysicians.model.Physician;
import leti_sisdis_6.happhysicians.model.Appointment;
import leti_sisdis_6.happhysicians.model.AppointmentStatus;
import leti_sisdis_6.happhysicians.model.ConsultationType;
import leti_sisdis_6.happhysicians.model.Specialty;
import leti_sisdis_6.happhysicians.model.Department;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.io.IOException;
import static org.mockito.Mockito.when;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "server.port=8080",
                "spring.profiles.active=test",
                "server.ssl.enabled=false",
                "spring.main.allow-bean-definition-overriding=true",
                // Disable security for Pact tests
                "spring.security.user.name=pact",
                "spring.security.user.password=",
                // Exclude MongoDB auto-configuration to avoid real connections
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration"
        }
)
@ComponentScan(
        basePackages = "leti_sisdis_6.happhysicians",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {leti_sisdis_6.happhysicians.config.SecurityConfig.class}
        )
)
@TestPropertySource(properties = {
        "spring.security.enabled=false"
})
@Provider("hap-physicians-service")
@PactFolder("../hap-patients/target/pacts")
public class PactProviderTest {

    // --- MOCKS DE INFRAESTRUTURA ---

    @MockBean
    private PhysicianRepository physicianRepository;

    @MockBean
    private AppointmentRepository appointmentRepository;

    @MockBean
    private MongoTemplate mongoTemplate;

    @MockBean
    private PhysicianQueryRepository physicianQueryRepository;

    @MockBean
    private AppointmentQueryRepository appointmentQueryRepository;

    @MockBean
    private PhysicianQueryService physicianQueryService;

    @MockBean
    private AppointmentQueryService appointmentQueryService;

    // Mock SecurityConfig to prevent it from being loaded
    @MockBean
    private SecurityConfig securityConfig;

    // -------------------------------

    // Provide PasswordEncoder bean needed by PhysicianService (required because SecurityConfig is excluded)
    @TestConfiguration
    static class PasswordEncoderConfig {
        @Bean
        @Primary
        public PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }

    @TestConfiguration
    @EnableWebSecurity
    @Order(1)
    static class TestSecurityConfig {
        @Bean
        @Primary
        @Order(1)
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            // Allow all requests but provide authentication for @PreAuthorize to work
            http.csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .oauth2ResourceServer(oauth2 -> oauth2.disable()) // Disable OAuth2 resource server
                    .httpBasic(httpBasic -> httpBasic.disable()) // Disable HTTP Basic
                    .formLogin(formLogin -> formLogin.disable()) // Disable form login
                    .logout(logout -> logout.disable()) // Disable logout
                    .addFilterBefore(new Filter() {
                        @Override
                        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                                throws IOException, ServletException {
                            // Provide authentication with all necessary authorities for Pact tests
                            SimpleGrantedAuthority adminAuth = new SimpleGrantedAuthority("ADMIN");
                            SimpleGrantedAuthority patientAuth = new SimpleGrantedAuthority("PATIENT");
                            SimpleGrantedAuthority physicianAuth = new SimpleGrantedAuthority("PHYSICIAN");
                            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                    "pact-test-user", 
                                    null, 
                                    java.util.Arrays.asList(adminAuth, patientAuth, physicianAuth)
                            );
                            SecurityContextHolder.getContext().setAuthentication(auth);
                            chain.doFilter(request, response);
                        }
                    }, org.springframework.security.web.authentication.AnonymousAuthenticationFilter.class);
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

    @State("physician with ID 1 exists")
    public void physicianExists() {
        Specialty specialty = Specialty.builder()
                .specialtyId("1")
                .name("Cardiology")
                .build();

        Department department = Department.builder()
                .departmentId("1")
                .code("INT")
                .name("Internal Medicine")
                .build();

        Physician mockPhysician = Physician.builder()
                .physicianId("1")
                .fullName("Dr. John Smith")
                .licenseNumber("LIC12345")
                .username("dr.smith@example.com")
                .password("encrypted")
                .specialty(specialty)
                .department(department)
                .build();

        // Mock JPA repository
        when(physicianRepository.findById("1")).thenReturn(Optional.of(mockPhysician));

        // Mock Query Service (used by controllers)
        when(physicianQueryService.getPhysicianById("1")).thenReturn(mockPhysician);
    }

    @State("appointment with ID 1 exists")
    public void appointmentExists() {
        Specialty specialty = Specialty.builder()
                .specialtyId("1")
                .name("Cardiology")
                .build();

        Department department = Department.builder()
                .departmentId("1")
                .code("INT")
                .name("Internal Medicine")
                .build();

        Physician physician = Physician.builder()
                .physicianId("1")
                .fullName("Dr. John Smith")
                .licenseNumber("LIC12345")
                .username("dr.smith@example.com")
                .specialty(specialty)
                .department(department)
                .build();

        Appointment mockAppointment = Appointment.builder()
                .appointmentId("1")
                .patientId("P001")
                .patientName("Jane Doe")
                .patientEmail("jane@example.com")
                .physician(physician)
                .dateTime(LocalDateTime.now().plusDays(1))
                .consultationType(ConsultationType.FIRST_TIME)
                .status(AppointmentStatus.SCHEDULED)
                .wasRescheduled(false)
                .build();

        // Mock JPA repository
        when(appointmentRepository.findById("1")).thenReturn(Optional.of(mockAppointment));

        // Mock Query Service (used by controllers)
        when(appointmentQueryService.getAppointmentById("1")).thenReturn(Optional.of(mockAppointment));
    }
}
