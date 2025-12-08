package leti_sisdis_6.happatients.api;

import leti_sisdis_6.happatients.dto.PatientDetailsDTO;
import leti_sisdis_6.happatients.dto.PatientProfileDTO;
import leti_sisdis_6.happatients.http.ResilientRestTemplate;
import leti_sisdis_6.happatients.repository.PatientLocalRepository;
import leti_sisdis_6.happatients.service.PatientQueryService;
import leti_sisdis_6.happatients.service.PatientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PatientController.class)
@AutoConfigureMockMvc
@ContextConfiguration(classes = {PatientSecurityTest.MinimalApp.class, PatientController.class})
@org.springframework.boot.autoconfigure.ImportAutoConfiguration({JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class})
class PatientSecurityTest {

    // 2. Definimos uma "Mini App" que desliga explicitamente as bases de dados
    @SpringBootApplication(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            MongoAutoConfiguration.class,
            MongoDataAutoConfiguration.class
    })
    static class MinimalApp {
        // Esta classe serve apenas para arrancar o contexto do Spring sem erros de DB
    }

    @Autowired
    private MockMvc mockMvc;

    // 3. Usamos @MockBean direto (mais limpo e seguro que classes estáticas)
    // O Spring encarrega-se de injetar isto no Controller automaticamente.

    @MockBean
    private JwtDecoder jwtDecoder; // Necessário para a segurança arrancar

    @MockBean
    private PatientService patientService;

    @MockBean
    private PatientLocalRepository localRepository;

    @MockBean
    private ResilientRestTemplate resilientRestTemplate;

    @MockBean // O tal que faltava e dava erro de UnsatisfiedDependencyException
    private PatientQueryService patientQueryService;

    @Test
    void listPatients_requiresAdmin() throws Exception {
        mockMvc.perform(get("/patients"))
                .andExpect(status().isUnauthorized());

        when(patientService.listAllPatients()).thenReturn(List.of(PatientDetailsDTO.builder()
                .patientId("PAT01").fullName("Alice").email("a@a").build()));

        mockMvc.perform(get("/patients")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .authorities(new SimpleGrantedAuthority("ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].patientId").value("PAT01"));
    }

    @Test
    void getPatientDetails_requiresAdmin() throws Exception {
        String id = "PAT01";

        mockMvc.perform(get("/patients/{id}", id))
                .andExpect(status().isUnauthorized());

        PatientDetailsDTO details = PatientDetailsDTO.builder()
                .patientId(id)
                .fullName("Local")
                .email("l@l")
                .build();

        // O controller tenta primeiro pelo CQRS (Mongo) via patientQueryService
        // Simulamos que não existe lá, para cair no fallback SQL
        when(patientQueryService.getPatientProfile(id)).thenThrow(new leti_sisdis_6.happatients.exceptions.NotFoundException("not found"));
        // Depois retorna do serviço local SQL
        when(patientService.getPatientDetails(id)).thenReturn(details);

        mockMvc.perform(get("/patients/{id}", id)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .authorities(new SimpleGrantedAuthority("ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(id));
    }

    @Test
    void getPatientProfile_requiresPhysician() throws Exception {
        String id = "PAT01";
        mockMvc.perform(get("/patients/{id}/profile", id))
                .andExpect(status().isUnauthorized());

        PatientProfileDTO profile = PatientProfileDTO.builder()
                .patientId(id)
                .fullName("Alice")
                .email("a@a")
                .build();

        when(patientService.getPatientProfile(any(), any())).thenReturn(profile);

        mockMvc.perform(get("/patients/{id}/profile", id)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .authorities(new SimpleGrantedAuthority("PHYSICIAN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(id));
    }

    @Test
    void updateMyContact_requiresPatient() throws Exception {
        String body = "{\n" +
                "  \"phoneNumber\": \"+351912345678\",\n" +
                "  \"address\": {\n" +
                "    \"street\": \"A\", \"city\": \"B\", \"postalCode\": \"C\", \"country\": \"D\"\n" +
                "  }\n" +
                "}";

        mockMvc.perform(patch("/patients/me").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());

        when(patientService.updateContactDetails(any(), any())).thenReturn("Contact details updated successfully.");

        mockMvc.perform(patch("/patients/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(csrf())
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .authorities(new SimpleGrantedAuthority("PATIENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Contact details updated successfully."));
    }
}