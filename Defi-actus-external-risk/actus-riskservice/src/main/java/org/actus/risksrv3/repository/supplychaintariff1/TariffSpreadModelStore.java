package org.actus.risksrv3.repository.supplychaintariff1;

import org.actus.risksrv3.models.supplychaintariff1.TariffSpreadModelData;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TariffSpreadModelStore
        extends MongoRepository<TariffSpreadModelData, String> {
}
