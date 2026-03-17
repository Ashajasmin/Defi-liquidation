package org.actus.risksrv3.repository.defiliquidation1;

import org.actus.risksrv3.models.defiliquidation1.HealthFactorModelData;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface HealthFactorModelStore extends MongoRepository<HealthFactorModelData, String> {
}
