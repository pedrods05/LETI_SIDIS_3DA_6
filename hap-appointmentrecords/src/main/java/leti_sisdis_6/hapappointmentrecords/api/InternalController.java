package leti_sisdis_6.hapappointmentrecords.api;

import leti_sisdis_6.hapappointmentrecords.model.Appointment;
import leti_sisdis_6.hapappointmentrecords.repository.AppointmentRepository;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/internal/appointments")
@RequiredArgsConstructor
@Hidden
public class InternalController {

    private final AppointmentRepository appointmentRepository;

    @GetMapping("/{appointmentId}")
    public ResponseEntity<Appointment> getAppointmentInternal(@PathVariable String appointmentId) {
        Optional<Appointment> appointment = appointmentRepository.findById(appointmentId);
        return appointment.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

