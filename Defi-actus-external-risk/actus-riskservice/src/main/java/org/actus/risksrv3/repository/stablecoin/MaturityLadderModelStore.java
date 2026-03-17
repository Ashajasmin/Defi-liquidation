package org.actus.risksrv3.repository.stablecoin;

import org.actus.risksrv3.models.stablecoin.MaturityLadderModelData;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MaturityLadderModelStore
        extends MongoRepository<MaturityLadderModelData, String> {
}
