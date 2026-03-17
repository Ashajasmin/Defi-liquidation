package org.actus.risksrv3.models.hybridtreasury1;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document(collection = "fairValueComplianceModels")
public class FairValueComplianceModelData {

    @Id
    private String riskFactorId;

    private String assetFairValueMOC;
    private String msciExclusionProbMOC;
    private double msciThreshold;
    private double materialityThreshold;
    private List<String> monitoringEventTimes;

    public FairValueComplianceModelData() {}

    public String getRiskFactorId() { return riskFactorId; }
    public void setRiskFactorId(String riskFactorId) { this.riskFactorId = riskFactorId; }

    public String getAssetFairValueMOC() { return assetFairValueMOC; }
    public void setAssetFairValueMOC(String assetFairValueMOC) { this.assetFairValueMOC = assetFairValueMOC; }

    public String getMsciExclusionProbMOC() { return msciExclusionProbMOC; }
    public void setMsciExclusionProbMOC(String msciExclusionProbMOC) { this.msciExclusionProbMOC = msciExclusionProbMOC; }

    public double getMsciThreshold() { return msciThreshold; }
    public void setMsciThreshold(double msciThreshold) { this.msciThreshold = msciThreshold; }

    public double getMaterialityThreshold() { return materialityThreshold; }
    public void setMaterialityThreshold(double materialityThreshold) { this.materialityThreshold = materialityThreshold; }

    public List<String> getMonitoringEventTimes() { return monitoringEventTimes; }
    public void setMonitoringEventTimes(List<String> monitoringEventTimes) { this.monitoringEventTimes = monitoringEventTimes; }
}
