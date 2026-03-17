package org.actus.risksrv3.repository.supplychaintariff1;

import org.actus.risksrv3.models.supplychaintariff1.WorkingCapitalStressModelData;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface WorkingCapitalStressModelStore
        extends MongoRepository<WorkingCapitalStressModelData, String> {
}
