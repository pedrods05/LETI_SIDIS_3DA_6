package leti_sisdis_6.happhysicians.api;

import leti_sisdis_6.happhysicians.dto.request.RegisterPhysicianRequest;
import leti_sisdis_6.happhysicians.dto.response.PhysicianFullDTO;
import leti_sisdis_6.happhysicians.dto.response.PhysicianLimitedDTO;
import leti_sisdis_6.happhysicians.model.Department;
import leti_sisdis_6.happhysicians.model.Physician;
import leti_sisdis_6.happhysicians.model.Specialty;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.List;

@Component
public class PhysicianMapper {

    public Physician toEntity(RegisterPhysicianRequest request, Department department, Specialty specialty) {
        return Physician.builder()
                .fullName(request.getFullName())
                .licenseNumber(request.getLicenseNumber())
                .username(request.getUsername())
                .specialty(specialty)
                .department(department)
                .emails(request.getEmails())
                .phoneNumbers(request.getPhoneNumbers())
                .workingHourStart(LocalTime.parse(request.getWorkingHourStart()))
                .workingHourEnd(LocalTime.parse(request.getWorkingHourEnd()))
                .build();
    }

    public PhysicianFullDTO toFullDTO(Physician physician) {
        return PhysicianFullDTO.builder()
                .physicianId(physician.getPhysicianId())
                .fullName(physician.getFullName())
                .licenseNumber(physician.getLicenseNumber())
                .username(physician.getUsername())
                .specialtyName(physician.getSpecialty().getName())
                .departmentName(physician.getDepartment().getName())
                .emails(physician.getEmails())
                .phoneNumbers(physician.getPhoneNumbers())
                .workingHourStart(physician.getWorkingHourStart())
                .workingHourEnd(physician.getWorkingHourEnd())
                .photo(physician.getPhoto() != null ? 
                    PhysicianFullDTO.PhotoDTO.builder()
                        .url(physician.getPhoto().getUrl())
                        .uploadedAt(physician.getPhoto().getUploadedAt())
                        .build() : null)
                .build();
    }

    public PhysicianLimitedDTO toLimitedDTO(Physician physician) {
        return PhysicianLimitedDTO.builder()
                .physicianId(physician.getPhysicianId())
                .fullName(physician.getFullName())
                .specialtyName(physician.getSpecialty().getName())
                .departmentName(physician.getDepartment().getName())
                .emails(physician.getEmails())
                .phoneNumbers(physician.getPhoneNumbers())
                .workingHourStart(physician.getWorkingHourStart())
                .workingHourEnd(physician.getWorkingHourEnd())
                .build();
    }

    public List<PhysicianLimitedDTO> toLimitedDTOList(List<Physician> physicians) {
        return physicians.stream()
                .map(this::toLimitedDTO)
                .toList();
    }
}