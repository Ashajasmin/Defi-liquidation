package org.actus.risksrv3.models.stablecoin;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

/**
 * AssetQualityModelData
 *
 * MongoDB document for AssetQualityModel configuration.
 * Collection: assetQualityModels
 *
 * JSON example:
 * {
 *   "riskFactorId": "aq_sc01",
 *   "bankStressIndexMOC": "BANK_STRESS_INDEX",
 *   "sovereignStressMOC": "US_SOVEREIGN_STRESS",
 *   "bankStressThreshold": 0.5,
 *   "baseQuality": 100.0,
 *   "qualityFloor": 50.0,
 *   "sovereignMaxDegradation": 0.30,
 *   "strictMode": false,
 *   "hqlaMOC": "SC_HQLA_SCORE",
 *   "monitoringEventTimes": ["2025-06-01T00:00:00", ...]
 * }
 *
 * BACKWARD COMPATIBLE: strictMode and hqlaMOC are optional.
 * If strictMode=true, enforces GENIUS Act §4(a)(1) requirement: HQLA must be 100 (all L1 assets).
 * If strictMode=false (default), uses qualityFloor (50) allowing L2A/L2B assets per Basel III.
 */
@Document(collection = "assetQualityModels")
public class AssetQualityModelData {

    @Id
    private String riskFactorId;

    private String bankStressIndexMOC;
    private String sovereignStressMOC;
    private double bankStressThreshold;
    private double baseQuality;
    private double qualityFloor;
    private double sovereignMaxDegradation;
    
    // GENIUS Act strict compliance mode (BACKWARD COMPATIBLE - defaults to false)
    private boolean strictMode = false;
    
    // Optional: Read HQLA score from published index (BACKWARD COMPATIBLE - can be null)
    private String hqlaMOC;
    
    private List<String> monitoringEventTimes;

    public AssetQualityModelData() {
    }

    public String getRiskFactorId() { return riskFactorId; }
    public void setRiskFactorId(String riskFactorId) { this.riskFactorId = riskFactorId; }

    public String getBankStressIndexMOC() { return bankStressIndexMOC; }
    public void setBankStressIndexMOC(String bankStressIndexMOC) { this.bankStressIndexMOC = bankStressIndexMOC; }

    public String getSovereignStressMOC() { return sovereignStressMOC; }
    public void setSovereignStressMOC(String sovereignStressMOC) { this.sovereignStressMOC = sovereignStressMOC; }

    public double getBankStressThreshold() { return bankStressThreshold; }
    public void setBankStressThreshold(double bankStressThreshold) { this.bankStressThreshold = bankStressThreshold; }

    public double getBaseQuality() { return baseQuality; }
    public void setBaseQuality(double baseQuality) { this.baseQuality = baseQuality; }

    public double getQualityFloor() { return qualityFloor; }
    public void setQualityFloor(double qualityFloor) { this.qualityFloor = qualityFloor; }

    public double getSovereignMaxDegradation() { return sovereignMaxDegradation; }
    public void setSovereignMaxDegradation(double sovereignMaxDegradation) { this.sovereignMaxDegradation = sovereignMaxDegradation; }

    public List<String> getMonitoringEventTimes() { return monitoringEventTimes; }
    public void setMonitoringEventTimes(List<String> monitoringEventTimes) { this.monitoringEventTimes = monitoringEventTimes; }

    // NEW: Getter/Setter for strictMode (BACKWARD COMPATIBLE)
    public boolean isStrictMode() { return strictMode; }
    public void setStrictMode(boolean strictMode) { this.strictMode = strictMode; }

    // NEW: Getter/Setter for hqlaMOC (BACKWARD COMPATIBLE)
    public String getHqlaMOC() { return hqlaMOC; }
    public void setHqlaMOC(String hqlaMOC) { this.hqlaMOC = hqlaMOC; }
}
