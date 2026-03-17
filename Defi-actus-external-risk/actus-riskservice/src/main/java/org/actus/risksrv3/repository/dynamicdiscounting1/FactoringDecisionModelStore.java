package org.actus.risksrv3.repository.dynamicdiscounting1;

import org.actus.risksrv3.models.dynamicdiscounting1.FactoringDecisionModelData;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FactoringDecisionModelStore
        extends MongoRepository<FactoringDecisionModelData, String> {
}
