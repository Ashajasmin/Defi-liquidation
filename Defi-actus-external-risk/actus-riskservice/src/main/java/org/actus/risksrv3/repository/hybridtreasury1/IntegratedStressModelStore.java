package org.actus.risksrv3.repository.hybridtreasury1;

import org.actus.risksrv3.models.hybridtreasury1.IntegratedStressModelData;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface IntegratedStressModelStore
        extends MongoRepository<IntegratedStressModelData, String> {
}
