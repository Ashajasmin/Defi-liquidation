package org.actus.risksrv3.repository.dynamicdiscounting1;

import org.actus.risksrv3.models.dynamicdiscounting1.SupplierUrgencyModelData;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SupplierUrgencyModelStore
        extends MongoRepository<SupplierUrgencyModelData, String> {
}
