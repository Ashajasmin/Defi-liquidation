package org.actus.risksrv3.repository.hybridtreasury1;

import org.actus.risksrv3.models.hybridtreasury1.FairValueComplianceModelData;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FairValueComplianceModelStore
        extends MongoRepository<FairValueComplianceModelData, String> {
}
