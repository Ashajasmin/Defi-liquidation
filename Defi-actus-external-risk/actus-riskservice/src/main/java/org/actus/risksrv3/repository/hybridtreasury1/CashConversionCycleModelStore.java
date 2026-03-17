package org.actus.risksrv3.repository.hybridtreasury1;

import org.actus.risksrv3.models.hybridtreasury1.CashConversionCycleModelData;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CashConversionCycleModelStore
        extends MongoRepository<CashConversionCycleModelData, String> {
}
