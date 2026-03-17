package org.actus.risksrv3.models.stablecoin;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

/**
 * ContinuousAttestationModelData
 *
 * MongoDB document for ContinuousAttestationModel configuration.
 * Collection: continuousAttestationModels
 *
 * JSON example:
 * {
 *   "riskFactorId": "ca_sc01",
 *   "backingRiskMOC": "SC_BACKING_RISK",
 *   "liquidityRiskMOC": "SC_LIQUIDITY_RISK",
 *   "qualityRiskMOC": "SC_QUALITY_RISK",
 *   "concentrationRiskMOC": "SC_CONCENTRATION_RISK",
 *   "complianceRiskMOC": "SC_COMPLIANCE_RISK",
 *   "earlyWarningRiskMOC": "SC_EARLYWARNING_RISK",
 *   "monitoringEventTimes": ["2025-06-01T00:00:00", ...]
 * }
 */
@Document(collection = "continuousAttestationModels")
public class ContinuousAttestationModelData {

    @Id
    private String riskFactorId;

    private String backingRiskMOC;
    private String liquidityRiskMOC;
    private String qualityRiskMOC;
    private String concentrationRiskMOC;
    private String complianceRiskMOC;
    private String earlyWarningRiskMOC;
    private List<String> monitoringEventTimes;

    public ContinuousAttestationModelData() {
    }

    public String getRiskFactorId() { return riskFactorId; }
    public void setRiskFactorId(String riskFactorId) { this.riskFactorId = riskFactorId; }

    public String getBackingRiskMOC() { return backingRiskMOC; }
    public void setBackingRiskMOC(String backingRiskMOC) { this.backingRiskMOC = backingRiskMOC; }

    public String getLiquidityRiskMOC() { return liquidityRiskMOC; }
    public void setLiquidityRiskMOC(String liquidityRiskMOC) { this.liquidityRiskMOC = liquidityRiskMOC; }

    public String getQualityRiskMOC() { return qualityRiskMOC; }
    public void setQualityRiskMOC(String qualityRiskMOC) { this.qualityRiskMOC = qualityRiskMOC; }

    public String getConcentrationRiskMOC() { return concentrationRiskMOC; }
    public void setConcentrationRiskMOC(String concentrationRiskMOC) { this.concentrationRiskMOC = concentrationRiskMOC; }

    public String getComplianceRiskMOC() { return complianceRiskMOC; }
    public void setComplianceRiskMOC(String complianceRiskMOC) { this.complianceRiskMOC = complianceRiskMOC; }

    public String getEarlyWarningRiskMOC() { return earlyWarningRiskMOC; }
    public void setEarlyWarningRiskMOC(String earlyWarningRiskMOC) { this.earlyWarningRiskMOC = earlyWarningRiskMOC; }

    public List<String> getMonitoringEventTimes() { return monitoringEventTimes; }
    public void setMonitoringEventTimes(List<String> monitoringEventTimes) { this.monitoringEventTimes = monitoringEventTimes; }
}
