package org.actus.risksrv3.repository.hybridtreasury1;

import org.actus.risksrv3.models.hybridtreasury1.YieldArbitrageModelData;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface YieldArbitrageModelStore
        extends MongoRepository<YieldArbitrageModelData, String> {
}
