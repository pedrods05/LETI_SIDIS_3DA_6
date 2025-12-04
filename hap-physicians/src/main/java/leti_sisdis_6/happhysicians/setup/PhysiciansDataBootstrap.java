package leti_sisdis_6.happhysicians.setup;

import leti_sisdis_6.happhysicians.model.Department;
import leti_sisdis_6.happhysicians.model.Specialty;
import leti_sisdis_6.happhysicians.model.Physician;
import leti_sisdis_6.happhysicians.repository.DepartmentRepository;
import leti_sisdis_6.happhysicians.repository.SpecialtyRepository;
import leti_sisdis_6.happhysicians.repository.PhysicianRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.boot.CommandLineRunner;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Order(2)
public class PhysiciansDataBootstrap implements CommandLineRunner {

    private final PhysicianRepository physicianRepository;
    private final DepartmentRepository departmentRepository;
    private final SpecialtyRepository specialtyRepository;

    @Override
    public void run(String... args) {
        if (physicianRepository.count() > 0) return;

        Department dep01 = departmentRepository.findById("DEP01").orElseThrow();
        Department dep02 = departmentRepository.findById("DEP02").orElseThrow();
        Department dep03 = departmentRepository.findById("DEP03").orElseThrow();
        Department dep04 = departmentRepository.findById("DEP04").orElseThrow();

        Specialty spc01 = specialtyRepository.findById("SPC01").orElseThrow();
        Specialty spc02 = specialtyRepository.findById("SPC02").orElseThrow();
        Specialty spc03 = specialtyRepository.findById("SPC03").orElseThrow();
        Specialty spc04 = specialtyRepository.findById("SPC04").orElseThrow();
        Specialty spc05 = specialtyRepository.findById("SPC05").orElseThrow();
        Specialty spc06 = specialtyRepository.findById("SPC06").orElseThrow();
        Specialty spc07 = specialtyRepository.findById("SPC07").orElseThrow();

        List<Physician> physicians = Arrays.asList(
                Physician.builder()
                        .physicianId("PHY01")
                        .fullName("Dr. João Silva")
                        .licenseNumber("12345")
                        .department(dep01)
                        .specialty(spc01)
                        .emails(List.of("joao.silva@hospital.com"))
                        .phoneNumbers(List.of("+351912345678"))
                        .workingHourStart(LocalTime.of(9,0))
                        .workingHourEnd(LocalTime.of(17,0))
                        .username("joao.silva@hospital.com")
                        .password("{noop}Password123!")
                        .build(),

                Physician.builder()
                        .physicianId("PHY02")
                        .fullName("Dra. Maria Santos")
                        .licenseNumber("67890")
                        .department(dep02)
                        .specialty(spc02)
                        .emails(List.of("maria.santos@hospital.com"))
                        .phoneNumbers(List.of("+351912345679"))
                        .workingHourStart(LocalTime.of(8,0))
                        .workingHourEnd(LocalTime.of(16,0))
                        .username("maria.santos@hospital.com")
                        .password("{noop}Password123!")
                        .build()

        );

        physicianRepository.saveAll(physicians);
    }
}
