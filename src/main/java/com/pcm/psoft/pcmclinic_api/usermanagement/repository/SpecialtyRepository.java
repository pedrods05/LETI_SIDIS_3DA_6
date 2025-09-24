package com.pcm.psoft.pcmclinic_api.usermanagement.repository;

import com.pcm.psoft.pcmclinic_api.usermanagement.model.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpecialtyRepository extends JpaRepository<Specialty, String> {
    boolean existsByName(String name);

}
