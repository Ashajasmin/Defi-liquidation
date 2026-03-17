package org.actus.risksrv3.repository.hybridtreasury1;

import org.actus.risksrv3.models.hybridtreasury1.PegStressModelData;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PegStressModelStore
        extends MongoRepository<PegStressModelData, String> {
}
