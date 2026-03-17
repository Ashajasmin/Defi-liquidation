package org.actus.risksrv3.repository.supplychaintariff1;

import org.actus.risksrv3.models.supplychaintariff1.PortCongestionModelData;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PortCongestionModelStore
        extends MongoRepository<PortCongestionModelData, String> {
}
