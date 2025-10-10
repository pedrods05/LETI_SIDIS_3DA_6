package leti_sisdis_6.happhysicians.dto.input;

import lombok.Data;
import java.util.List;

@Data
public class UpdatePhysicianRequest {
    private String fullName;
    private String licenseNumber;
    private String specialtyId;
    private String departmentId;
    private List<String> emails;
    private List<String> phoneNumbers;
    private String workingHourStart;
    private String workingHourEnd;
}
