package org.actus.risksrv3.repository.defiliquidation1;

import org.actus.risksrv3.models.defiliquidation1.CascadeProbabilityModelData;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CascadeProbabilityModelStore extends MongoRepository<CascadeProbabilityModelData, String> {
}
