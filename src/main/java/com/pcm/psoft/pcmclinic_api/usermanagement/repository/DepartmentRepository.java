package com.pcm.psoft.pcmclinic_api.usermanagement.repository;

import com.pcm.psoft.pcmclinic_api.usermanagement.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, String> {
    boolean existsByCode(String code);

}
