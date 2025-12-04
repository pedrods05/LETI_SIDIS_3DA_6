package leti_sisdis_6.happhysicians.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PhysicianUpdatedEvent implements Serializable {
    private String physicianId;
    private String fullName;
    private String licenseNumber;
    private String username;
    private String specialtyId;
    private String specialtyName;
    private String departmentId;
    private String departmentName;
}

