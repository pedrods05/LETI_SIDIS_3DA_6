package leti_sisdis_6.happhysicians;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhysicianLimitedDTO {
    private String physicianId;
    private String fullName;
    private String specialtyName;
    private String departmentName;
    private List<String> emails;
    private List<String> phoneNumbers;
    private LocalTime workingHourStart;
    private LocalTime workingHourEnd;
}
