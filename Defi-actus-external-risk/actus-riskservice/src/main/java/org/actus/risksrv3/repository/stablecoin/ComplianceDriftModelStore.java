package org.actus.risksrv3.repository.stablecoin;

import org.actus.risksrv3.models.stablecoin.ComplianceDriftModelData;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ComplianceDriftModelStore
        extends MongoRepository<ComplianceDriftModelData, String> {
}
