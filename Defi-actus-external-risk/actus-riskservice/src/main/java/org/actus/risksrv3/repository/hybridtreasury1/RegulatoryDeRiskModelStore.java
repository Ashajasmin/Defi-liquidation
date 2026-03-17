package org.actus.risksrv3.repository.hybridtreasury1;

import org.actus.risksrv3.models.hybridtreasury1.RegulatoryDeRiskModelData;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RegulatoryDeRiskModelStore
        extends MongoRepository<RegulatoryDeRiskModelData, String> {
}
