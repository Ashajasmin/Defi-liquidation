package org.actus.risksrv3.models.hybridtreasury1;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document(collection = "regulatoryDeRiskModels")
public class RegulatoryDeRiskModelData {

    @Id
    private String riskFactorId;

    private String regulatoryStressMOC;
    private String marketDepthMOC;
    private double regulatoryThreshold;
    private double dailyLiquidationFraction;
    private int maxDays;
    private double minMarketDepth;
    private List<String> monitoringEventTimes;

    public RegulatoryDeRiskModelData() {}

    public String getRiskFactorId() { return riskFactorId; }
    public void setRiskFactorId(String riskFactorId) { this.riskFactorId = riskFactorId; }

    public String getRegulatoryStressMOC() { return regulatoryStressMOC; }
    public void setRegulatoryStressMOC(String regulatoryStressMOC) { this.regulatoryStressMOC = regulatoryStressMOC; }

    public String getMarketDepthMOC() { return marketDepthMOC; }
    public void setMarketDepthMOC(String marketDepthMOC) { this.marketDepthMOC = marketDepthMOC; }

    public double getRegulatoryThreshold() { return regulatoryThreshold; }
    public void setRegulatoryThreshold(double regulatoryThreshold) { this.regulatoryThreshold = regulatoryThreshold; }

    public double getDailyLiquidationFraction() { return dailyLiquidationFraction; }
    public void setDailyLiquidationFraction(double dailyLiquidationFraction) { this.dailyLiquidationFraction = dailyLiquidationFraction; }

    public int getMaxDays() { return maxDays; }
    public void setMaxDays(int maxDays) { this.maxDays = maxDays; }

    public double getMinMarketDepth() { return minMarketDepth; }
    public void setMinMarketDepth(double minMarketDepth) { this.minMarketDepth = minMarketDepth; }

    public List<String> getMonitoringEventTimes() { return monitoringEventTimes; }
    public void setMonitoringEventTimes(List<String> monitoringEventTimes) { this.monitoringEventTimes = monitoringEventTimes; }
}
