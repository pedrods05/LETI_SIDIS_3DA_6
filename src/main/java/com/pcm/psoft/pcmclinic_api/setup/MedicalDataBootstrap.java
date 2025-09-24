package com.pcm.psoft.pcmclinic_api.setup;

import com.pcm.psoft.pcmclinic_api.usermanagement.model.Department;
import com.pcm.psoft.pcmclinic_api.usermanagement.model.Specialty;
import com.pcm.psoft.pcmclinic_api.usermanagement.repository.DepartmentRepository;
import com.pcm.psoft.pcmclinic_api.usermanagement.repository.SpecialtyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(1)
public class MedicalDataBootstrap implements CommandLineRunner {

    private final DepartmentRepository departmentRepository;
    private final SpecialtyRepository specialtyRepository;

    @Override
    public void run(String... args) {
        preloadDepartments();
        preloadSpecialties();
    }

    private void preloadDepartments() {
        createDepartment("DEP01", "CARD", "Cardiologia", "Cuidados com o coração e sistema cardiovascular");
        createDepartment("DEP02", "NEUR", "Neurologia", "Tratamento do cérebro e sistema nervoso");
        createDepartment("DEP03", "ORTH", "Ortopedia", "Cuidados com o sistema músculo-esquelético");
        createDepartment("DEP04", "GAST", "Gastroenterologia", "Cuidados com o sistema digestivo e gastrointestinal");
    }


    private void preloadSpecialties() {
        createSpecialty("SPC01", "Cardiologia");
        createSpecialty("SPC02", "Neurologia");
        createSpecialty("SPC03", "Ortopedia");
        createSpecialty("SPC04", "Pediatria");
        createSpecialty("SPC05", "Dermatologia");
        createSpecialty("SPC06", "Medicina Geral");
        createSpecialty("SPC07", "Gastroenterologia");
    }

    private void createDepartment(String id, String code, String name, String description) {
        if (!departmentRepository.existsByCode(code)) {
            Department department = new Department(id, code, name, description);
            departmentRepository.save(department);
        }
    }

    private void createSpecialty(String id, String name) {
        if (!specialtyRepository.existsByName(name)) {
            Specialty specialty = new Specialty(id, name);
            specialtyRepository.save(specialty);
        }
    }
}
