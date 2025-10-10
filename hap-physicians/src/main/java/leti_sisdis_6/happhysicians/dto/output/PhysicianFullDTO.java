package leti_sisdis_6.happhysicians.dto.output;

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
public class PhysicianFullDTO {
    private String fullName;
    private String specialtyId;
    private String specialtyName;
    private String departmentId;
    private String departmentName;
    private ContactInfoDTO contactInfo;
    private WorkingHoursDTO workingHours;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContactInfoDTO {
        private List<String> emails;
        private List<String> phoneNumbers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkingHoursDTO {
        private LocalTime start;
        private LocalTime end;
    }
}
