package leti_sisdis_6.happhysicians.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
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
