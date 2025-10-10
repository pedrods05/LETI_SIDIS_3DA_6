package leti_sisdis_6.happhysicians.api;

import leti_sisdis_6.happhysicians.dto.input.RegisterPhysicianRequest;
import leti_sisdis_6.happhysicians.dto.output.PhysicianIdResponse;
import leti_sisdis_6.happhysicians.dto.output.PhysicianFullDTO;
import leti_sisdis_6.happhysicians.dto.output.PhysicianLimitedDTO;
import leti_sisdis_6.happhysicians.model.Department;
import leti_sisdis_6.happhysicians.model.Physician;
import leti_sisdis_6.happhysicians.model.Specialty;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface PhysicianMapper {

    @Mappings({
            @Mapping(target = "physicianId", ignore = true),
            @Mapping(target = "specialty", source = "specialty"),
            @Mapping(target = "department", source = "department")
    })
    Physician toEntity(RegisterPhysicianRequest request, Department department, Specialty specialty);

    @Mappings({
        @Mapping(target = "specialtyId", source = "specialty.specialtyId"),
        @Mapping(target = "specialtyName", source = "specialty.name"),
        @Mapping(target = "departmentId", source = "department.departmentId"),
        @Mapping(target = "departmentName", source = "department.name"),
        @Mapping(target = "contactInfo.emails", source = "emails"),
        @Mapping(target = "contactInfo.phoneNumbers", source = "phoneNumbers"),
        @Mapping(target = "workingHours.start", source = "workingHourStart"),
        @Mapping(target = "workingHours.end", source = "workingHourEnd")
    })
    PhysicianFullDTO toFullDTO(Physician physician);

    @Mappings({
        @Mapping(target = "specialtyId", source = "specialty.specialtyId"),
            @Mapping(target = "specialtyName", source = "specialty.name")
    })
    PhysicianLimitedDTO toLimitedDTO(Physician physician);

    default PhysicianIdResponse toResponse(Physician physician) {
        PhysicianIdResponse.PhotoDTO photoDTO = null;
        if (physician.getPhoto() != null) {
            photoDTO = new PhysicianIdResponse.PhotoDTO(
                    physician.getPhoto().getUrl(),
                    physician.getPhoto().getUploadedAt()
            );
        }
        return new PhysicianIdResponse(
                physician.getPhysicianId(),
                "Physician registered successfully.",
                photoDTO
        );
    }



    default List<PhysicianLimitedDTO> toLimitedDTOList(List<Physician> physicians) {
        return physicians.stream()
                .map(this::toLimitedDTO)
                .collect(Collectors.toList());
    }
}
