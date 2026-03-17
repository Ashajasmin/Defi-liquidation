package org.actus.risksrv3.repository.dynamicdiscounting1;

import org.actus.risksrv3.models.dynamicdiscounting1.OptimalPaymentTimingModelData;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OptimalPaymentTimingModelStore
        extends MongoRepository<OptimalPaymentTimingModelData, String> {
}
