package org.actus.risksrv3.repository.hybridtreasury1;

import org.actus.risksrv3.models.hybridtreasury1.AllocationDriftModelData;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * AllocationDriftModelStore
 *
 * Spring Data MongoDB repository for AllocationDriftModelData documents.
 * Inherits CRUD: save, findById, findAll, deleteById, existsById.
 *
 * Injected into:
 *   - RiskDataManager       (CRUD REST endpoints)
 *   - RiskObservationHandler (scenario initialization)
 */
public interface AllocationDriftModelStore
        extends MongoRepository<AllocationDriftModelData, String> {
}
