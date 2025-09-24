package com.pcm.psoft.pcmclinic_api.usermanagement.api;

import com.pcm.psoft.pcmclinic_api.usermanagement.dto.input.RegisterPhysicianRequest;
import com.pcm.psoft.pcmclinic_api.usermanagement.dto.output.PhysicianIdResponse;
import com.pcm.psoft.pcmclinic_api.usermanagement.dto.output.PhysicianFullDTO;
import com.pcm.psoft.pcmclinic_api.usermanagement.dto.output.PhysicianLimitedDTO;
import com.pcm.psoft.pcmclinic_api.usermanagement.model.Department;
import com.pcm.psoft.pcmclinic_api.usermanagement.model.Physician;
import com.pcm.psoft.pcmclinic_api.usermanagement.model.Specialty;
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
