package org.actus.risksrv3.repository.hybridtreasury1;

import org.actus.risksrv3.models.hybridtreasury1.ScheduledCashFlowModelData;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * ScheduledCashFlowModelStore
 *
 * Spring Data MongoDB repository for ScheduledCashFlowModelData.
 * Collection: scheduledCashFlowModels
 * ID type: String (riskFactorId)
 */
public interface ScheduledCashFlowModelStore
        extends MongoRepository<ScheduledCashFlowModelData, String> {
}
