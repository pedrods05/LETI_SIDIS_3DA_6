package leti_sisdis_6.happhysicians.config;

import leti_sisdis_6.happhysicians.model.Appointment;
import leti_sisdis_6.happhysicians.model.AppointmentStatus;
import leti_sisdis_6.happhysicians.model.ConsultationType;
import leti_sisdis_6.happhysicians.model.Department;
import leti_sisdis_6.happhysicians.model.Physician;
import leti_sisdis_6.happhysicians.model.Specialty;
import leti_sisdis_6.happhysicians.repository.AppointmentRepository;
import leti_sisdis_6.happhysicians.repository.DepartmentRepository;
import leti_sisdis_6.happhysicians.repository.PhysicianRepository;
import leti_sisdis_6.happhysicians.repository.SpecialtyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private SpecialtyRepository specialtyRepository;

    @Autowired
    private PhysicianRepository physicianRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Override
    public void run(String... args) throws Exception {
        // Create departments
        Department cardiology = Department.builder()
                .departmentId("CARD")
                .code("CARD")
                .name("Cardiology")
                .description("Heart and cardiovascular system")
                .build();

        Department neurology = Department.builder()
                .departmentId("NEUR")
                .code("NEUR")
                .name("Neurology")
                .description("Brain and nervous system")
                .build();

        Department orthopedics = Department.builder()
                .departmentId("ORTH")
                .code("ORTH")
                .name("Orthopedics")
                .description("Bones and musculoskeletal system")
                .build();

        departmentRepository.saveAll(Arrays.asList(cardiology, neurology, orthopedics));

        // Create specialties
        Specialty cardiologist = Specialty.builder()
                .specialtyId("CARDIO")
                .name("Cardiologist")
                .build();

        Specialty neurologist = Specialty.builder()
                .specialtyId("NEURO")
                .name("Neurologist")
                .build();

        Specialty orthopedic = Specialty.builder()
                .specialtyId("ORTHO")
                .name("Orthopedic Surgeon")
                .build();

        specialtyRepository.saveAll(Arrays.asList(cardiologist, neurologist, orthopedic));

        // Create physicians
        Physician dr1 = Physician.builder()
                .physicianId("PHY001")
                .fullName("Dr. João Silva")
                .licenseNumber("12345")
                .username("joao.silva@hospital.com")
                .password("password123")
                .specialty(cardiologist)
                .department(cardiology)
                .emails(Arrays.asList("joao.silva@hospital.com", "joao.silva@email.com"))
                .phoneNumbers(Arrays.asList("+351912345678", "+351213456789"))
                .workingHourStart(LocalTime.of(9, 0))
                .workingHourEnd(LocalTime.of(17, 0))
                .build();

        Physician dr2 = Physician.builder()
                .physicianId("PHY002")
                .fullName("Dr. Maria Santos")
                .licenseNumber("67890")
                .username("maria.santos@hospital.com")
                .password("password123")
                .specialty(neurologist)
                .department(neurology)
                .emails(Arrays.asList("maria.santos@hospital.com"))
                .phoneNumbers(Arrays.asList("+351912345679"))
                .workingHourStart(LocalTime.of(8, 0))
                .workingHourEnd(LocalTime.of(16, 0))
                .build();

        Physician dr3 = Physician.builder()
                .physicianId("PHY003")
                .fullName("Dr. Carlos Oliveira")
                .licenseNumber("54321")
                .username("carlos.oliveira@hospital.com")
                .password("password123")
                .specialty(orthopedic)
                .department(orthopedics)
                .emails(Arrays.asList("carlos.oliveira@hospital.com"))
                .phoneNumbers(Arrays.asList("+351912345680"))
                .workingHourStart(LocalTime.of(10, 0))
                .workingHourEnd(LocalTime.of(18, 0))
                .build();

        physicianRepository.saveAll(Arrays.asList(dr1, dr2, dr3));

        // Create test appointments with current dates
        LocalDateTime now = LocalDateTime.now();
        Appointment appointment1 = Appointment.builder()
                .appointmentId("APT001")
                .patientId("PAT001")
                .physician(dr1)
                .dateTime(now.plusDays(1).withHour(10).withMinute(0))
                .consultationType(ConsultationType.FIRST_TIME)
                .status(AppointmentStatus.SCHEDULED)
                .wasRescheduled(false)
                .build();

        Appointment appointment2 = Appointment.builder()
                .appointmentId("APT002")
                .patientId("PAT002")
                .physician(dr2)
                .dateTime(now.plusDays(2).withHour(11).withMinute(0))
                .consultationType(ConsultationType.FOLLOW_UP)
                .status(AppointmentStatus.SCHEDULED)
                .wasRescheduled(false)
                .build();

        Appointment appointment3 = Appointment.builder()
                .appointmentId("APT003")
                .patientId("PAT003")
                .physician(dr1)
                .dateTime(now.plusDays(3).withHour(14).withMinute(0))
                .consultationType(ConsultationType.FIRST_TIME)
                .status(AppointmentStatus.COMPLETED)
                .wasRescheduled(false)
                .build();

        appointmentRepository.saveAll(Arrays.asList(appointment1, appointment2, appointment3));
        System.out.println("=== DataInitializer COMPLETED - Created 3 appointments ===");
    }
}
