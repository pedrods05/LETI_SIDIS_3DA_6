package leti_sisdis_6.happhysicians.setup;

// ...existing code...

import leti_sisdis_6.happhysicians.model.Appointment;
import leti_sisdis_6.happhysicians.model.AppointmentStatus;
import leti_sisdis_6.happhysicians.model.ConsultationType;
import leti_sisdis_6.happhysicians.model.Physician;
import leti_sisdis_6.happhysicians.repository.AppointmentRepository;
import leti_sisdis_6.happhysicians.repository.PhysicianRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Order(99) // move late and effectively no-op now
public class AppointmentsDataBootstrap implements CommandLineRunner {

    private final AppointmentRepository appointmentRepository;
    private final PhysicianRepository physicianRepository;

    @Override
    public void run(String... args) {
        // Physicians now fetch appointments from hap-appointmentrecords (8083).
        // Keep this bootstrap disabled to avoid duplicated/conflicting data.
        return;
    }
}
