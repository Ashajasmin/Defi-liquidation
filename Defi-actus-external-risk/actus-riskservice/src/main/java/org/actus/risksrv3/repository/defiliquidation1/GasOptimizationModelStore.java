package org.actus.risksrv3.repository.defiliquidation1;

import org.actus.risksrv3.models.defiliquidation1.GasOptimizationModelData;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GasOptimizationModelStore extends MongoRepository<GasOptimizationModelData, String> {
}
