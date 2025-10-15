package leti_sisdis_6.hapappointmentrecords.dto.output;

import com.pcm.psoft.pcmclinic_api.appointment.model.AppointmentStatus;
import com.pcm.psoft.pcmclinic_api.appointment.model.ConsultationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class AppointmentHistoryDTO {
    private String appointmentId;
    private LocalDateTime dateTime;
    private ConsultationType consultationType;
    private AppointmentStatus status;
    private AppointmentRecordDTO record;

    @Data
    @Builder
    public static class AppointmentRecordDTO {
        private String diagnosis;
        private String treatmentRecommendations;
        private String prescriptions;
        private LocalTime duration;
    }
} 