package leti_sisdis_6.happatients.dto;

import leti_sisdis_6.happatients.dto.external.appointments.AppointmentRecordViewDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientProfileDTO {
    private PatientDetailsDTO patient;
    private List<AppointmentRecordViewDTO> appointmentHistory;
}

