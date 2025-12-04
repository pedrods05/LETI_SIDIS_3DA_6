package leti_sisdis_6.happhysicians.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppointmentCanceledEvent implements Serializable {
    private String appointmentId;
    private String patientId;
    private String physicianId;
}

