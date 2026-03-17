package org.actus.risksrv3.models.hybridtreasury1;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document(collection = "yieldArbitrageModels")
public class YieldArbitrageModelData {

    @Id
    private String riskFactorId;

    private String tbillYieldMOC;
    private String stakingYieldMOC;
    private String lendingYieldMOC;
    private double minSpreadBps;
    private double riskAdjustmentFactor;
    private List<String> monitoringEventTimes;

    public YieldArbitrageModelData() {}

    public String getRiskFactorId() { return riskFactorId; }
    public void setRiskFactorId(String riskFactorId) { this.riskFactorId = riskFactorId; }

    public String getTbillYieldMOC() { return tbillYieldMOC; }
    public void setTbillYieldMOC(String tbillYieldMOC) { this.tbillYieldMOC = tbillYieldMOC; }

    public String getStakingYieldMOC() { return stakingYieldMOC; }
    public void setStakingYieldMOC(String stakingYieldMOC) { this.stakingYieldMOC = stakingYieldMOC; }

    public String getLendingYieldMOC() { return lendingYieldMOC; }
    public void setLendingYieldMOC(String lendingYieldMOC) { this.lendingYieldMOC = lendingYieldMOC; }

    public double getMinSpreadBps() { return minSpreadBps; }
    public void setMinSpreadBps(double minSpreadBps) { this.minSpreadBps = minSpreadBps; }

    public double getRiskAdjustmentFactor() { return riskAdjustmentFactor; }
    public void setRiskAdjustmentFactor(double riskAdjustmentFactor) { this.riskAdjustmentFactor = riskAdjustmentFactor; }

    public List<String> getMonitoringEventTimes() { return monitoringEventTimes; }
    public void setMonitoringEventTimes(List<String> monitoringEventTimes) { this.monitoringEventTimes = monitoringEventTimes; }
}
