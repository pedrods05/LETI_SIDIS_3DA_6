package leti_sisdis_6.happhysicians.api;


import leti_sisdis_6.happhysicians.dto.output.AppointmentDetailsDTO;
import leti_sisdis_6.happhysicians.dto.output.AppointmentListDTO;
import leti_sisdis_6.happhysicians.dto.output.PatientAppointmentDTO;
import leti_sisdis_6.happhysicians.dto.output.PhysicianAverageDTO;
import leti_sisdis_6.happhysicians.model.Appointment;
import lombok.Builder;
import org.springframework.stereotype.Component;

import java.util.List;
@Builder
@Component
public class AppointmentMapper {

    public List<AppointmentListDTO> toListDTO(List<Appointment> appointments) {
        return appointments.stream()
                .map(this::toListDTO)
                .toList();
    }

    private AppointmentListDTO toListDTO(Appointment appointment) {
        return AppointmentListDTO.builder()
                .date(appointment.getDateTime().toLocalDate())
                .time(appointment.getDateTime().toLocalTime())
                .patientName(appointment.getPatientName())
                .physicianName(appointment.getPhysician().getFullName())
                .build();
    }

    public AppointmentDetailsDTO toDetailsDTO(Appointment appointment) {
        return AppointmentDetailsDTO.builder()
                .appointmentId(appointment.getAppointmentId())
                .patientId(appointment.getPatientId())
                .patientName(appointment.getPatientName())
                .physicianId(appointment.getPhysician().getPhysicianId())
                .physicianName(appointment.getPhysician().getFullName())
                .dateTime(appointment.getDateTime())
                .consultationType(appointment.getConsultationType())
                .status(appointment.getStatus())
                .build();
    }

    public PatientAppointmentDTO toPatientDTO(Appointment appointment) {
        return PatientAppointmentDTO.builder()
                .physicianName(appointment.getPhysician().getFullName())
                .dateTime(appointment.getDateTime())
                .consultationType(appointment.getConsultationType())
                .status(appointment.getStatus())
                .build();
    }

    public List<PhysicianAverageDTO> toAverageDTOList(List<Object[]> results) {
        return results.stream()
                .map(this::toAverageDTO)
                .toList();
    }

    private PhysicianAverageDTO toAverageDTO(Object[] result) {
        Double averageMinutes = ((Number) result[2]).doubleValue();
        return PhysicianAverageDTO.builder()
                .physicianId((String) result[0])
                .physicianName((String) result[1])
                .averageDuration(String.format("%.0f min", averageMinutes))
                .build();
    }
}
