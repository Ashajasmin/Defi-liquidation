package org.actus.risksrv3.repository.dynamicdiscounting1;

import org.actus.risksrv3.models.dynamicdiscounting1.PenaltyAccrualModelData;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PenaltyAccrualModelStore
        extends MongoRepository<PenaltyAccrualModelData, String> {
}
