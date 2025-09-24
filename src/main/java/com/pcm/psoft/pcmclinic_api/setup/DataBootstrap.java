package com.pcm.psoft.pcmclinic_api.setup;

import com.pcm.psoft.pcmclinic_api.appointment.model.Appointment;
import com.pcm.psoft.pcmclinic_api.appointment.model.AppointmentRecord;
import com.pcm.psoft.pcmclinic_api.appointment.model.AppointmentStatus;
import com.pcm.psoft.pcmclinic_api.appointment.model.ConsultationType;
import com.pcm.psoft.pcmclinic_api.appointment.repository.AppointmentRecordRepository;
import com.pcm.psoft.pcmclinic_api.appointment.repository.AppointmentRepository;
import com.pcm.psoft.pcmclinic_api.patient.model.Address;
import com.pcm.psoft.pcmclinic_api.patient.model.InsuranceInfo;
import com.pcm.psoft.pcmclinic_api.patient.model.Patient;
import com.pcm.psoft.pcmclinic_api.patient.repository.PatientRepository;
import com.pcm.psoft.pcmclinic_api.usermanagement.model.Department;
import com.pcm.psoft.pcmclinic_api.usermanagement.model.Physician;
import com.pcm.psoft.pcmclinic_api.usermanagement.model.Specialty;
import com.pcm.psoft.pcmclinic_api.usermanagement.model.User;
import com.pcm.psoft.pcmclinic_api.usermanagement.model.Role;
import com.pcm.psoft.pcmclinic_api.usermanagement.repository.DepartmentRepository;
import com.pcm.psoft.pcmclinic_api.usermanagement.repository.PhysicianRepository;
import com.pcm.psoft.pcmclinic_api.usermanagement.repository.SpecialtyRepository;
import com.pcm.psoft.pcmclinic_api.usermanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Order(2)
public class DataBootstrap implements CommandLineRunner {

    private final PhysicianRepository physicianRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentRecordRepository appointmentRecordRepository;
    private final DepartmentRepository departmentRepository;
    private final SpecialtyRepository specialtyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Carregar todos os dados de uma vez
        loadPhysicians();
        loadPatients();
        loadAppointments();
    }

    private void loadPhysicians() {

        List<Physician> physicians = Arrays.asList(
            createPhysician("PHY01", "Dr. João Silva", "12345",
                specialtyRepository.findById("SPC01").orElseThrow(),
                departmentRepository.findById("DEP01").orElseThrow(),
                Arrays.asList("joao.silva@hospital.com"), Arrays.asList("+351912345678"),
                LocalTime.of(9, 0), LocalTime.of(17, 0)),

            createPhysician("PHY02", "Dra. Maria Santos", "67890",
                specialtyRepository.findById("SPC02").orElseThrow(),
                departmentRepository.findById("DEP02").orElseThrow(),
                Arrays.asList("maria.santos@hospital.com"), Arrays.asList("+351912345679"),
                LocalTime.of(8, 0), LocalTime.of(16, 0)),

            createPhysician("PHY03", "Dr. Pedro Oliveira", "54321",
                specialtyRepository.findById("SPC03").orElseThrow(),
                departmentRepository.findById("DEP03").orElseThrow(),
                Arrays.asList("pedro.oliveira@hospital.com"), Arrays.asList("+351912345680"),
                LocalTime.of(10, 0), LocalTime.of(18, 0)),

            createPhysician("PHY04", "Dra. Ana Costa", "98765",
                specialtyRepository.findById("SPC04").orElseThrow(),
                departmentRepository.findById("DEP01").orElseThrow(),
                Arrays.asList("ana.costa@hospital.com"), Arrays.asList("+351912345681"),
                LocalTime.of(9, 0), LocalTime.of(17, 0)),

            createPhysician("PHY05", "Dr. Carlos Mendes", "45678",
                specialtyRepository.findById("SPC05").orElseThrow(),
                departmentRepository.findById("DEP03").orElseThrow(),
                Arrays.asList("carlos.mendes@hospital.com"), Arrays.asList("+351912345682"),
                LocalTime.of(8, 0), LocalTime.of(16, 0)),

            createPhysician("PHY06", "Dra. Sofia Rodrigues", "23456",
                specialtyRepository.findById("SPC06").orElseThrow(),
                departmentRepository.findById("DEP04").orElseThrow(),
                Arrays.asList("sofia.rodrigues@hospital.com"), Arrays.asList("+351912345683"),
                LocalTime.of(10, 0), LocalTime.of(18, 0)),

            createPhysician("PHY07", "Dr. Ricardo Alves", "87654",
                specialtyRepository.findById("SPC07").orElseThrow(),
                departmentRepository.findById("DEP04").orElseThrow(),
                Arrays.asList("ricardo.alves@hospital.com"), Arrays.asList("+351912345684"),
                LocalTime.of(9, 0), LocalTime.of(17, 0))
        );

        physicianRepository.saveAll(physicians);
    }

    private void loadPatients() {
        List<Patient> patients = Arrays.asList(
            createPatient("PAT01", "Ana Costa", "ana.costa@email.com", "+351912345681",
                LocalDate.of(1985, 5, 15), "Rua das Flores, 123", "Lisboa", "1000-100", "Portugal",
                "MED2024001", "Medicare Seguros", "COMPREHENSIVE"),

            createPatient("PAT02", "Carlos Mendes", "carlos.mendes@email.com", "+351912345682",
                LocalDate.of(1990, 8, 20), "Avenida da Liberdade, 45", "Porto", "4000-000", "Portugal",
                "SAUDE2024002", "Saúde Total Seguros", "FAMILY"),

            createPatient("PAT03", "Sofia Rodrigues", "sofia.rodrigues@email.com", "+351912345683",
                LocalDate.of(1978, 3, 10), "Rua do Comércio, 78", "Braga", "4700-000", "Portugal",
                "NONE", "NONE", "NONE")
        );

        patientRepository.saveAll(patients);
    }

    private void loadAppointments() {
        List<Physician> physicians = physicianRepository.findAll();
        List<Patient> patients = patientRepository.findAll();

        // Arrays de dados para gerar registros mais realistas
        String[] diagnoses = {
            "Hipertensão arterial",
            "Diabetes tipo 2",
            "Artrite reumatoide",
            "Asma brônquica",
            "Depressão",
            "Ansiedade",
            "Dor lombar crônica",
            "Enxaqueca",
            "Gastrite",
            "Insônia"
        };

        String[] treatments = {
            "Medicação prescrita: Losartana 50mg, 1 comprimido ao dia",
            "Medicação prescrita: Metformina 850mg, 2x ao dia",
            "Medicação prescrita: Ibuprofeno 600mg, 3x ao dia",
            "Medicação prescrita: Salbutamol, uso conforme necessidade",
            "Medicação prescrita: Sertralina 50mg, 1 comprimido ao dia",
            "Medicação prescrita: Alprazolam 0.25mg, 2x ao dia",
            "Fisioterapia 2x por semana, exercícios de fortalecimento",
            "Medicação prescrita: Sumatriptana 50mg, uso conforme necessidade",
            "Medicação prescrita: Omeprazol 20mg, 1 comprimido ao dia",
            "Higiene do sono, evitar cafeína após 16h"
        };

        String[] recommendations = {
            "Retornar em 3 meses para reavaliação",
            "Retornar em 1 mês para ajuste de medicação",
            "Retornar em 2 meses para avaliação de progresso",
            "Retornar em 6 meses para controle",
            "Retornar em 1 mês para acompanhamento",
            "Retornar em 2 semanas para avaliação de ansiedade",
            "Continuar exercícios em casa, retornar em 1 mês",
            "Manter diário de crises, retornar em 2 meses",
            "Evitar alimentos ácidos, retornar em 3 meses",
            "Manter diário do sono, retornar em 1 mês"
        };

        // Criar 10 consultas por paciente
        LocalDateTime now = LocalDateTime.now();
        int appointmentCounter = 1;

        for (Patient patient : patients) {
            // Criar 10 consultas para cada paciente
            for (int i = 0; i < 10; i++) {

                Physician physician = physicians.get(i % physicians.size());


                LocalDateTime baseDateTime = now.minusMonths(10 - i);


                int dayOfWeek = baseDateTime.getDayOfWeek().getValue();
                if (dayOfWeek == 7) { // Domingo
                    baseDateTime = baseDateTime.plusDays(1); // Mover para segunda-feira
                }

                int hour;
                if (dayOfWeek == 6) {
                    // Sábado: apenas manhã (9h-13h)
                    hour = 9 + (i % 4);
                } else {
                    // Segunda a sexta: manhã (9h-13h) ou tarde (14h-20h)
                    hour = (i % 2 == 0) ? 9 + (i % 4) : 14 + (i % 6);
                }

                LocalDateTime appointmentDateTime = baseDateTime
                    .withHour(hour)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0);

                // Definir status da consulta
                AppointmentStatus status = AppointmentStatus.COMPLETED;

                Appointment appointment = Appointment.builder()
                    .appointmentId(String.format("APT%02d", appointmentCounter++))
                    .patient(patient)
                    .physician(physician)
                    .dateTime(appointmentDateTime)
                    .consultationType(i == 0 ? ConsultationType.FIRST_TIME : ConsultationType.FOLLOW_UP)
                    .status(status)
                    .build();

                appointment = appointmentRepository.save(appointment);

                // Criar registro da consulta
                int index = i % diagnoses.length;
                AppointmentRecord record = AppointmentRecord.builder()
                    .recordId(String.format("REC%02d", appointmentCounter - 1))
                    .appointment(appointment)
                    .diagnosis(diagnoses[index])
                    .treatmentRecommendations(recommendations[index])
                    .prescriptions(treatments[index])
                    .duration(LocalTime.of(0, 20)) // Duração fixa de 20 minutos
                    .build();

                appointmentRecordRepository.save(record);
            }
        }

        // Criar consultas futuras
        createFutureAppointments(patients, physicians, appointmentCounter);
    }

    private void createFutureAppointments(List<Patient> patients, List<Physician> physicians, int startCounter) {
        LocalDateTime now = LocalDateTime.now();
        int appointmentCounter = startCounter;

        for (Patient patient : patients) {

            for (int i = 0; i < 3; i++) {
                Physician physician = physicians.get(i % physicians.size());


                LocalDateTime baseDateTime = now.plusMonths(i + 1);

                int dayOfWeek = baseDateTime.getDayOfWeek().getValue();
                if (dayOfWeek == 7) {
                    baseDateTime = baseDateTime.plusDays(1); // Mover para segunda-feira
                }

                int hour;
                if (dayOfWeek == 6) {
                    hour = 9 + (i % 4);
                } else {
                    hour = (i % 2 == 0) ? 9 + (i % 4) : 14 + (i % 6);
                }

                LocalDateTime appointmentDateTime = baseDateTime
                    .withHour(hour)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0);

                Appointment appointment = Appointment.builder()
                    .appointmentId(String.format("APT%02d", appointmentCounter++))
                    .patient(patient)
                    .physician(physician)
                    .dateTime(appointmentDateTime)
                    .consultationType(ConsultationType.FOLLOW_UP)
                    .status(AppointmentStatus.SCHEDULED)
                    .build();

                appointmentRepository.save(appointment);
            }
        }
    }

    private Physician createPhysician(String id, String fullName, String licenseNumber,
                                    Specialty specialty, Department department,
                                    List<String> emails, List<String> phoneNumbers,
                                    LocalTime startTime, LocalTime endTime) {
        // Criar usuário para o médico
        User user = new User();
        user.setId(id);
        user.setUsername(emails.get(0));
        user.setPassword(passwordEncoder.encode("Password123!"));
        user.setRole(Role.PHYSICIAN);
        userRepository.save(user);

        return Physician.builder()
            .physicianId(id)
            .fullName(fullName)
            .licenseNumber(licenseNumber)
            .specialty(specialty)
            .department(department)
            .emails(emails)
            .phoneNumbers(phoneNumbers)
            .workingHourStart(startTime)
            .workingHourEnd(endTime)
            .username(emails.get(0))
            .password(passwordEncoder.encode("Password123!"))
            .build();
    }

    private Patient createPatient(String id, String fullName, String email, String phoneNumber,
                                LocalDate birthDate, String street, String city,
                                String postalCode, String country,
                                String policyNumber, String provider, String coverageType) {
        User user = new User();
        user.setId(id);
        user.setUsername(email);
        user.setPassword(passwordEncoder.encode("Password123!"));
        user.setRole(Role.PATIENT);
        userRepository.save(user);

        Address address = Address.builder()
            .id("ADR" + id.substring(3))
            .street(street)
            .city(city)
            .postalCode(postalCode)
            .country(country)
            .build();

        InsuranceInfo insuranceInfo = InsuranceInfo.builder()
            .id("INS" + id.substring(3))
            .policyNumber(policyNumber)
            .provider(provider)
            .coverageType(coverageType)
            .build();

        return Patient.builder()
            .patientId(id)
            .fullName(fullName)
            .email(email)
            .phoneNumber(phoneNumber)
            .birthDate(birthDate)
            .address(address)
            .insuranceInfo(insuranceInfo)
            .dataConsentGiven(true)
            .dataConsentDate(LocalDate.now())
            .build();
    }
}
