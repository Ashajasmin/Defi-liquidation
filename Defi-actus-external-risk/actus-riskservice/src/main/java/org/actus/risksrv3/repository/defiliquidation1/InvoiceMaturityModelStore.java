package org.actus.risksrv3.repository.defiliquidation1;

import org.actus.risksrv3.models.defiliquidation1.InvoiceMaturityModelData;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface InvoiceMaturityModelStore extends MongoRepository<InvoiceMaturityModelData, String> {
}
