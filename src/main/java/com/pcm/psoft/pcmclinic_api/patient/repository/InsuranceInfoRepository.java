package com.pcm.psoft.pcmclinic_api.patient.repository;

import com.pcm.psoft.pcmclinic_api.patient.model.InsuranceInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InsuranceInfoRepository extends JpaRepository<InsuranceInfo, String> {
}
