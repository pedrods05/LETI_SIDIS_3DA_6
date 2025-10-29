package leti_sisdis_6.happatients.setup;

import leti_sisdis_6.happatients.model.Patient;
// Se o teu modelo tiver Address/InsuranceInfo locais, descomenta:
// import leti_sisdis_6.happatients.model.Address;
// import leti_sisdis_6.happatients.model.InsuranceInfo;

import leti_sisdis_6.happatients.repository.PatientRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.boot.CommandLineRunner;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Order(1)
public class DataBootstrap implements CommandLineRunner {

    private final PatientRepository patientRepository;

    @Override
    public void run(String... args) {
        if (patientRepository.count() > 0) return; // idempotente

        // Exemplo simples; adapta aos campos do teu modelo local
        List<Patient> patients = Arrays.asList(
                Patient.builder()
                        .patientId("PAT01")
                        .fullName("Ana Costa")
                        .email("ana.costa@email.com")
                        .phoneNumber("+351912345681")
                        .birthDate(LocalDate.of(1985, 5, 15))
                        // .address(Address.builder().street("Rua das Flores, 123").city("Lisboa").postalCode("1000-100").country("Portugal").build())
                        // .insuranceInfo(InsuranceInfo.builder().policyNumber("MED2024001").provider("Medicare Seguros").coverageType("COMPREHENSIVE").build())
                        .dataConsentGiven(true)
                        .dataConsentDate(LocalDate.now())
                        .build(),

                Patient.builder()
                        .patientId("PAT02")
                        .fullName("Carlos Mendes")
                        .email("carlos.mendes@email.com")
                        .phoneNumber("+351912345682")
                        .birthDate(LocalDate.of(1990, 8, 20))
                        .dataConsentGiven(true)
                        .dataConsentDate(LocalDate.now())
                        .build()
        );

        patientRepository.saveAll(patients);
    }
}
