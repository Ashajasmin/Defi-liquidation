package org.actus.risksrv3.repository.defiliquidation1;

import org.actus.risksrv3.models.defiliquidation1.CollateralVelocityModelData;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CollateralVelocityModelStore extends MongoRepository<CollateralVelocityModelData, String> {
}
