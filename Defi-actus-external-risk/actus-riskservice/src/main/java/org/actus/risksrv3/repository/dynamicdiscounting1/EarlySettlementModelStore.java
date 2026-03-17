package org.actus.risksrv3.repository.dynamicdiscounting1;

import org.actus.risksrv3.models.dynamicdiscounting1.EarlySettlementModelData;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EarlySettlementModelStore
        extends MongoRepository<EarlySettlementModelData, String> {
}
