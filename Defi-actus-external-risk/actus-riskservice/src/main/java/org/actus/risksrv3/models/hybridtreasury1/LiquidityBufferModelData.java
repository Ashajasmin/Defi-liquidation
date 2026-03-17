package org.actus.risksrv3.models.hybridtreasury1;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document(collection = "liquidityBufferModels")
public class LiquidityBufferModelData {

    @Id
    private String riskFactorId;

    private String projectedOutflowsMOC;
    private String tbillMaturityScheduleMOC;
    private double minBufferUSD;
    private double targetBufferUSD;
    private List<String> monitoringEventTimes;

    public LiquidityBufferModelData() {}

    public String getRiskFactorId() { return riskFactorId; }
    public void setRiskFactorId(String riskFactorId) { this.riskFactorId = riskFactorId; }

    public String getProjectedOutflowsMOC() { return projectedOutflowsMOC; }
    public void setProjectedOutflowsMOC(String projectedOutflowsMOC) { this.projectedOutflowsMOC = projectedOutflowsMOC; }

    public String getTbillMaturityScheduleMOC() { return tbillMaturityScheduleMOC; }
    public void setTbillMaturityScheduleMOC(String tbillMaturityScheduleMOC) { this.tbillMaturityScheduleMOC = tbillMaturityScheduleMOC; }

    public double getMinBufferUSD() { return minBufferUSD; }
    public void setMinBufferUSD(double minBufferUSD) { this.minBufferUSD = minBufferUSD; }

    public double getTargetBufferUSD() { return targetBufferUSD; }
    public void setTargetBufferUSD(double targetBufferUSD) { this.targetBufferUSD = targetBufferUSD; }

    public List<String> getMonitoringEventTimes() { return monitoringEventTimes; }
    public void setMonitoringEventTimes(List<String> monitoringEventTimes) { this.monitoringEventTimes = monitoringEventTimes; }
}
