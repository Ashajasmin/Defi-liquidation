package org.actus.risksrv3.repository.stablecoin;

import org.actus.risksrv3.models.stablecoin.EarlyWarningModelData;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EarlyWarningModelStore
        extends MongoRepository<EarlyWarningModelData, String> {
}
