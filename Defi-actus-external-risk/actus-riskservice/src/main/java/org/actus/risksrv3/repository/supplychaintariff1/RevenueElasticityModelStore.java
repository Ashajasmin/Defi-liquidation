package org.actus.risksrv3.repository.supplychaintariff1;

import org.actus.risksrv3.models.supplychaintariff1.RevenueElasticityModelData;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RevenueElasticityModelStore
        extends MongoRepository<RevenueElasticityModelData, String> {
}
