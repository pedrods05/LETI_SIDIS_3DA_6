package leti_sisdis_6.happatients.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PatientRegisteredEvent implements Serializable {
    private String patientId;
    private String name;
    private String email;
}
