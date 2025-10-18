package leti_sisdis_6.happatients.dto;

import com.fasterxml.jackson.databind.JsonNode;
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
    private List<JsonNode> appointmentHistory;
}
