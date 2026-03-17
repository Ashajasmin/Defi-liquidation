package org.actus.risksrv3.repository.dynamicdiscounting1;

import org.actus.risksrv3.models.dynamicdiscounting1.CashPoolOptimizationModelData;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CashPoolOptimizationModelStore
        extends MongoRepository<CashPoolOptimizationModelData, String> {
}
