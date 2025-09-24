package com.pcm.psoft.pcmclinic_api.patient.repository;

import com.pcm.psoft.pcmclinic_api.patient.model.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, String> {
} 