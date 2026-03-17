package org.actus.risksrv3.repository.defiliquidation1;

import org.actus.risksrv3.models.defiliquidation1.CollateralRebalancingModelData;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CollateralRebalancingModelStore extends MongoRepository<CollateralRebalancingModelData, String> {
}
