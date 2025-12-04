package leti_sisdis_6.happhysicians.setup;

import leti_sisdis_6.happhysicians.model.Appointment;
import leti_sisdis_6.happhysicians.model.AppointmentStatus;
import leti_sisdis_6.happhysicians.model.ConsultationType;
import leti_sisdis_6.happhysicians.model.Physician;
import leti_sisdis_6.happhysicians.repository.AppointmentRepository;
import leti_sisdis_6.happhysicians.repository.PhysicianRepository;
import leti_sisdis_6.happhysicians.services.ExternalServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@RequiredArgsConstructor
@Order(3)
@Profile("seed-appointments")
public class AppointmentsDataBootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AppointmentsDataBootstrap.class);

    private final AppointmentRepository appointmentRepository;
    private final PhysicianRepository physicianRepository;
    private final ExternalServiceClient externalServiceClient;

    @Override
    public void run(String... args) {
        if (appointmentRepository.existsById("APT06")) {
            log.info("[Seed] Scheduled appointments already present (APT06 exists) - skipping seeding.");
            return;
        }

        List<String> patientIds = Arrays.asList("PAT01", "PAT02");
        List<String> physicianIds = Arrays.asList("PHY01", "PHY02");

        AtomicInteger counter = new AtomicInteger(6);

        String patId2 = patientIds.get(1);
        for (int i = 0; i < 5; i++) {
            String phyId = physicianIds.get(i % physicianIds.size());
            Optional<Physician> physicianOpt = physicianRepository.findById(phyId);
            if (physicianOpt.isEmpty()) continue;

            LocalDateTime dt = LocalDateTime.now()
                    .plusDays(1 + i * 7)
                    .withHour(10 + (i % 6))
                    .withMinute(0).withSecond(0).withNano(0);

            int n = counter.getAndIncrement();

            Appointment.AppointmentBuilder b = Appointment.builder()
                    .appointmentId(String.format("APT%02d", n))
                    .patientId(patId2)
                    .physician(physicianOpt.get())
                    .dateTime(dt)
                    .consultationType(ConsultationType.FOLLOW_UP)
                    .status(AppointmentStatus.SCHEDULED)
                    .wasRescheduled(false);

            try {
                Map<String, Object> patient = externalServiceClient.getPatientById(patId2);
                b.patientName((String) patient.get("fullName"));
                b.patientEmail((String) patient.get("email"));
                b.patientPhone((String) patient.get("phoneNumber"));
            } catch (Exception e) {
                b.patientName("Carlos Mendes");
                b.patientEmail("carlos.mendes@email.com");
                b.patientPhone("+351912345682");
            }

            appointmentRepository.save(b.build());
        }
        log.info("[Seed] Inserted scheduled appointments APT06..APT10 for patient {}", patId2);
    }
}
