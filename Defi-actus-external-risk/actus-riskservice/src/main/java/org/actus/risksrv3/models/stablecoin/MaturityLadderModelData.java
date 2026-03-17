package org.actus.risksrv3.models.stablecoin;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

/**
 * MaturityLadderModelData
 *
 * MongoDB document for MaturityLadderModel configuration.
 * Collection: maturityLadderModels
 *
 * JSON example:
 * {
 *   "riskFactorId": "ml_sc01",
 *   "pegRiskScoreMOC": "PEG_RISK_SCORE",
 *   "monitoringEventTimes": ["2025-06-28T00:00:00", ...]
 * }
 */
@Document(collection = "maturityLadderModels")
public class MaturityLadderModelData {

    @Id
    private String riskFactorId;

    private String pegRiskScoreMOC;
    private List<String> monitoringEventTimes;

    public MaturityLadderModelData() {
    }

    public String getRiskFactorId() {
        return riskFactorId;
    }

    public void setRiskFactorId(String riskFactorId) {
        this.riskFactorId = riskFactorId;
    }

    public String getPegRiskScoreMOC() {
        return pegRiskScoreMOC;
    }

    public void setPegRiskScoreMOC(String pegRiskScoreMOC) {
        this.pegRiskScoreMOC = pegRiskScoreMOC;
    }

    public List<String> getMonitoringEventTimes() {
        return monitoringEventTimes;
    }

    public void setMonitoringEventTimes(List<String> monitoringEventTimes) {
        this.monitoringEventTimes = monitoringEventTimes;
    }
}
