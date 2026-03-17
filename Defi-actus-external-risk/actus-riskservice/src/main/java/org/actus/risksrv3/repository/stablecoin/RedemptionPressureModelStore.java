package org.actus.risksrv3.repository.stablecoin;

import org.actus.risksrv3.models.stablecoin.RedemptionPressureModelData;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RedemptionPressureModelStore
        extends MongoRepository<RedemptionPressureModelData, String> {
}
