package org.actus.risksrv3.models.stablecoin;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

/**
 * ConcentrationDriftModelData
 *
 * MongoDB document for ConcentrationDriftModel configuration.
 * Collection: concentrationDriftModels
 *
 * JSON example:
 * {
 *   "riskFactorId": "cd_sc01",
 *   "assetBucketMOCs": ["SC_BUCKET_CASH", "SC_BUCKET_4W_TBILL",
 *                        "SC_BUCKET_13W_TBILL", "SC_BUCKET_26W_TBILL"],
 *   "custodianBucketMOCs": ["SC_CUSTODIAN_BOFA", "SC_CUSTODIAN_JPM",
 *                            "SC_CUSTODIAN_CITI", "SC_CUSTODIAN_WF"],
 *   "maxSingleAssetShare": 0.40,
 *   "hhiWarningThreshold": 0.35,
 *   "monitoringEventTimes": ["2025-06-01T00:00:00", ...]
 * }
 *
 * BACKWARD COMPATIBLE: custodianBucketMOCs is optional.
 * If provided, calculates custodian HHI to prevent SVB-style concentration.
 * If null or empty, only asset-type HHI is calculated (existing behavior).
 */
@Document(collection = "concentrationDriftModels")
public class ConcentrationDriftModelData {

    @Id
    private String riskFactorId;

    private List<String> assetBucketMOCs;
    
    // Optional: Custodian concentration tracking (BACKWARD COMPATIBLE - can be null)
    private List<String> custodianBucketMOCs;
    
    private double maxSingleAssetShare;
    private double hhiWarningThreshold;
    private List<String> monitoringEventTimes;

    public ConcentrationDriftModelData() {
    }

    public String getRiskFactorId() { return riskFactorId; }
    public void setRiskFactorId(String riskFactorId) { this.riskFactorId = riskFactorId; }

    public List<String> getAssetBucketMOCs() { return assetBucketMOCs; }
    public void setAssetBucketMOCs(List<String> assetBucketMOCs) { this.assetBucketMOCs = assetBucketMOCs; }

    // NEW: Getter/Setter for custodianBucketMOCs (BACKWARD COMPATIBLE)
    public List<String> getCustodianBucketMOCs() { return custodianBucketMOCs; }
    public void setCustodianBucketMOCs(List<String> custodianBucketMOCs) { this.custodianBucketMOCs = custodianBucketMOCs; }

    public double getMaxSingleAssetShare() { return maxSingleAssetShare; }
    public void setMaxSingleAssetShare(double maxSingleAssetShare) { this.maxSingleAssetShare = maxSingleAssetShare; }

    public double getHhiWarningThreshold() { return hhiWarningThreshold; }
    public void setHhiWarningThreshold(double hhiWarningThreshold) { this.hhiWarningThreshold = hhiWarningThreshold; }

    public List<String> getMonitoringEventTimes() { return monitoringEventTimes; }
    public void setMonitoringEventTimes(List<String> monitoringEventTimes) { this.monitoringEventTimes = monitoringEventTimes; }
}
