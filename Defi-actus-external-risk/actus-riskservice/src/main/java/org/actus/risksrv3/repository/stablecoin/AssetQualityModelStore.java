package org.actus.risksrv3.repository.stablecoin;

import org.actus.risksrv3.models.stablecoin.AssetQualityModelData;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AssetQualityModelStore
        extends MongoRepository<AssetQualityModelData, String> {
}
