package org.actus.risksrv3.repository;

import org.actus.risksrv3.models.CollateralLTVModelData;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * CollateralLTVModelStore
 *
 * Spring Data MongoDB repository for CollateralLTVModelData documents.
 *
 * Provides out-of-the-box CRUD operations inherited from MongoRepository:
 *   - save(CollateralLTVModelData)
 *   - findById(String id)          → Optional<CollateralLTVModelData>
 *   - findAll()                    → List<CollateralLTVModelData>
 *   - deleteById(String id)
 *   - existsById(String id)
 *
 * Injected into:
 *   - RiskDataManager    (CRUD REST endpoints)
 *   - RiskObservationHandler (scenario initialisation)
 *
 * No custom query methods are needed — the inherited MongoRepository
 * interface is sufficient for all current use cases.
 *
 * Place this file in:
 *   actus-riskservice/src/main/java/org/actus/risksrv3/repository/
 */
public interface CollateralLTVModelStore
        extends MongoRepository<CollateralLTVModelData, String> {

    // All required methods are inherited from MongoRepository<T, ID>.
    // Add custom @Query methods here if needed in the future.
}