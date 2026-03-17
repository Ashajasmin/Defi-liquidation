package org.actus.risksrv3.repository.defiliquidation1;

import org.actus.risksrv3.models.defiliquidation1.CorrelationRiskModelData;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CorrelationRiskModelStore extends MongoRepository<CorrelationRiskModelData, String> {
}
