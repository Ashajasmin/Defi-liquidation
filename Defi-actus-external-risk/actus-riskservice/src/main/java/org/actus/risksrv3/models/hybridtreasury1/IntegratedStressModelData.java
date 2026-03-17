package org.actus.risksrv3.models.hybridtreasury1;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document(collection = "integratedStressModels")
public class IntegratedStressModelData {

    @Id
    private String riskFactorId;

    private String compositeStressIndexMOC;
    private double lowThreshold;
    private double highThreshold;
    private double cascadeMultiplier;
    private List<String> monitoringEventTimes;

    public IntegratedStressModelData() {}

    public String getRiskFactorId() { return riskFactorId; }
    public void setRiskFactorId(String riskFactorId) { this.riskFactorId = riskFactorId; }

    public String getCompositeStressIndexMOC() { return compositeStressIndexMOC; }
    public void setCompositeStressIndexMOC(String compositeStressIndexMOC) { this.compositeStressIndexMOC = compositeStressIndexMOC; }

    public double getLowThreshold() { return lowThreshold; }
    public void setLowThreshold(double lowThreshold) { this.lowThreshold = lowThreshold; }

    public double getHighThreshold() { return highThreshold; }
    public void setHighThreshold(double highThreshold) { this.highThreshold = highThreshold; }

    public double getCascadeMultiplier() { return cascadeMultiplier; }
    public void setCascadeMultiplier(double cascadeMultiplier) { this.cascadeMultiplier = cascadeMultiplier; }

    public List<String> getMonitoringEventTimes() { return monitoringEventTimes; }
    public void setMonitoringEventTimes(List<String> monitoringEventTimes) { this.monitoringEventTimes = monitoringEventTimes; }
}
