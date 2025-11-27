package leti_sisdis_6.happhysicians.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "physician_summaries")
public class PhysicianSummary {
    @Id
    private String id;
    private String fullName;
    private String licenseNumber;
    private String username;
    private String specialtyId;
    private String specialtyName;
    private String departmentId;
    private String departmentName;
}

