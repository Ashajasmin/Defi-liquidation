package org.actus.risksrv3.repository.stablecoin;

import org.actus.risksrv3.models.stablecoin.ConcentrationDriftModelData;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ConcentrationDriftModelStore
        extends MongoRepository<ConcentrationDriftModelData, String> {
}
