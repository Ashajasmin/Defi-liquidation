package org.actus.risksrv3.models.hybridtreasury1;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document(collection = "pegStressModels")
public class PegStressModelData {

    @Id
    private String riskFactorId;

    private String primaryPegDeviationMOC;
    private String altPegDeviationMOC;
    private double pegDeviationThreshold;
    private double criticalDeviation;
    private List<String> monitoringEventTimes;

    public PegStressModelData() {}

    public String getRiskFactorId() { return riskFactorId; }
    public void setRiskFactorId(String riskFactorId) { this.riskFactorId = riskFactorId; }

    public String getPrimaryPegDeviationMOC() { return primaryPegDeviationMOC; }
    public void setPrimaryPegDeviationMOC(String primaryPegDeviationMOC) { this.primaryPegDeviationMOC = primaryPegDeviationMOC; }

    public String getAltPegDeviationMOC() { return altPegDeviationMOC; }
    public void setAltPegDeviationMOC(String altPegDeviationMOC) { this.altPegDeviationMOC = altPegDeviationMOC; }

    public double getPegDeviationThreshold() { return pegDeviationThreshold; }
    public void setPegDeviationThreshold(double pegDeviationThreshold) { this.pegDeviationThreshold = pegDeviationThreshold; }

    public double getCriticalDeviation() { return criticalDeviation; }
    public void setCriticalDeviation(double criticalDeviation) { this.criticalDeviation = criticalDeviation; }

    public List<String> getMonitoringEventTimes() { return monitoringEventTimes; }
    public void setMonitoringEventTimes(List<String> monitoringEventTimes) { this.monitoringEventTimes = monitoringEventTimes; }
}
