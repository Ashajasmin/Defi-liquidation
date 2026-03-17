package org.actus.risksrv3.models.defiliquidation1;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document(collection = "correlationRiskModels")
public class CorrelationRiskModelData {
    @Id
    private String riskFactorId;
    private String asset1MOC;
    private String asset2MOC;
    private double asset1Quantity;
    private double asset2Quantity;
    private double correlationThreshold;
    private double diversificationHaircut;
    private double baseLtvThreshold;
    private double liquidationThreshold;
    private int rollingWindowSize;
    private List<String> monitoringEventTimes;

    public CorrelationRiskModelData() {}
    public String getRiskFactorId() { return riskFactorId; }
    public void setRiskFactorId(String v) { this.riskFactorId = v; }
    public String getAsset1MOC() { return asset1MOC; }
    public void setAsset1MOC(String v) { this.asset1MOC = v; }
    public String getAsset2MOC() { return asset2MOC; }
    public void setAsset2MOC(String v) { this.asset2MOC = v; }
    public double getAsset1Quantity() { return asset1Quantity; }
    public void setAsset1Quantity(double v) { this.asset1Quantity = v; }
    public double getAsset2Quantity() { return asset2Quantity; }
    public void setAsset2Quantity(double v) { this.asset2Quantity = v; }
    public double getCorrelationThreshold() { return correlationThreshold; }
    public void setCorrelationThreshold(double v) { this.correlationThreshold = v; }
    public double getDiversificationHaircut() { return diversificationHaircut; }
    public void setDiversificationHaircut(double v) { this.diversificationHaircut = v; }
    public double getBaseLtvThreshold() { return baseLtvThreshold; }
    public void setBaseLtvThreshold(double v) { this.baseLtvThreshold = v; }
    public double getLiquidationThreshold() { return liquidationThreshold; }
    public void setLiquidationThreshold(double v) { this.liquidationThreshold = v; }
    public int getRollingWindowSize() { return rollingWindowSize; }
    public void setRollingWindowSize(int v) { this.rollingWindowSize = v; }
    public List<String> getMonitoringEventTimes() { return monitoringEventTimes; }
    public void setMonitoringEventTimes(List<String> v) { this.monitoringEventTimes = v; }
}