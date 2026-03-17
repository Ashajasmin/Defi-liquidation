package org.actus.risksrv3.models.defiliquidation1;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document(collection = "collateralVelocityModels")
public class CollateralVelocityModelData {
    @Id
    private String riskFactorId;
    private String collateralPriceMOC;
    private double collateralQuantity;
    private double liquidationThreshold;
    private double safeHorizonDays;
    private double urgentDays;
    private double moderateRepayFraction;
    private double aggressiveRepayFraction;
    private int rollingWindowSize;
    private List<String> monitoringEventTimes;

    public CollateralVelocityModelData() {}
    public String getRiskFactorId() { return riskFactorId; }
    public void setRiskFactorId(String v) { this.riskFactorId = v; }
    public String getCollateralPriceMOC() { return collateralPriceMOC; }
    public void setCollateralPriceMOC(String v) { this.collateralPriceMOC = v; }
    public double getCollateralQuantity() { return collateralQuantity; }
    public void setCollateralQuantity(double v) { this.collateralQuantity = v; }
    public double getLiquidationThreshold() { return liquidationThreshold; }
    public void setLiquidationThreshold(double v) { this.liquidationThreshold = v; }
    public double getSafeHorizonDays() { return safeHorizonDays; }
    public void setSafeHorizonDays(double v) { this.safeHorizonDays = v; }
    public double getUrgentDays() { return urgentDays; }
    public void setUrgentDays(double v) { this.urgentDays = v; }
    public double getModerateRepayFraction() { return moderateRepayFraction; }
    public void setModerateRepayFraction(double v) { this.moderateRepayFraction = v; }
    public double getAggressiveRepayFraction() { return aggressiveRepayFraction; }
    public void setAggressiveRepayFraction(double v) { this.aggressiveRepayFraction = v; }
    public int getRollingWindowSize() { return rollingWindowSize; }
    public void setRollingWindowSize(int v) { this.rollingWindowSize = v; }
    public List<String> getMonitoringEventTimes() { return monitoringEventTimes; }
    public void setMonitoringEventTimes(List<String> v) { this.monitoringEventTimes = v; }
}