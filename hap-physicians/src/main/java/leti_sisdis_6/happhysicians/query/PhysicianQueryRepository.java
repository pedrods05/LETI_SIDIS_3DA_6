package leti_sisdis_6.happhysicians.query;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhysicianQueryRepository extends MongoRepository<PhysicianSummary, String> {
}

