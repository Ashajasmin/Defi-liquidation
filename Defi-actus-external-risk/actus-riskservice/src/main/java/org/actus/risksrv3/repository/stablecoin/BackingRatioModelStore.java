package org.actus.risksrv3.repository.stablecoin;

import org.actus.risksrv3.models.stablecoin.BackingRatioModelData;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * BackingRatioModelStore
 *
 * Spring Data MongoDB repository for BackingRatioModelData documents.
 * Inherits CRUD: save, findById, findAll, deleteById, existsById.
 *
 * Injected into:
 *   - RiskDataManager       (CRUD REST endpoints)
 *   - RiskObservationHandler (scenario initialization)
 */
public interface BackingRatioModelStore
        extends MongoRepository<BackingRatioModelData, String> {
}
