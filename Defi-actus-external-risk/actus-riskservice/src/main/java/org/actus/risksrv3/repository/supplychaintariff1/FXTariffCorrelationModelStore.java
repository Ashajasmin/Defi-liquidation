package org.actus.risksrv3.repository.supplychaintariff1;

import org.actus.risksrv3.models.supplychaintariff1.FXTariffCorrelationModelData;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FXTariffCorrelationModelStore
        extends MongoRepository<FXTariffCorrelationModelData, String> {
}
