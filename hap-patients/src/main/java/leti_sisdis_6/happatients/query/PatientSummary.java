package leti_sisdis_6.happatients.query;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "patient_summaries")
public class PatientSummary {
    @Id
    private String id;
    private String name;
    private String email;
}
