package org.actus.risksrv3.repository.stablecoin;

import org.actus.risksrv3.models.stablecoin.ContinuousAttestationModelData;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ContinuousAttestationModelStore
        extends MongoRepository<ContinuousAttestationModelData, String> {
}
