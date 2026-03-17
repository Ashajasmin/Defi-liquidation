package org.actus.risksrv3.repository.hybridtreasury1;

import org.actus.risksrv3.models.hybridtreasury1.LiquidityBufferModelData;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface LiquidityBufferModelStore
        extends MongoRepository<LiquidityBufferModelData, String> {
}
