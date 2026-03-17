package org.actus.risksrv3.models.defiliquidation1;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document(collection = "healthFactorModels")
public class HealthFactorModelData {

    @Id
    private String riskFactorId;
    private List<String> collateralMOCs;
    private List<Double> collateralQuantities;
    private List<Double> liquidationThresholds;
    private double healthyThreshold;
    private double targetHealthFactor;
    private List<String> monitoringEventTimes;

    public HealthFactorModelData() {}

    public String getRiskFactorId() { return riskFactorId; }
    public void setRiskFactorId(String riskFactorId) { this.riskFactorId = riskFactorId; }
    public List<String> getCollateralMOCs() { return collateralMOCs; }
    public void setCollateralMOCs(List<String> collateralMOCs) { this.collateralMOCs = collateralMOCs; }
    public List<Double> getCollateralQuantities() { return collateralQuantities; }
    public void setCollateralQuantities(List<Double> collateralQuantities) { this.collateralQuantities = collateralQuantities; }
    public List<Double> getLiquidationThresholds() { return liquidationThresholds; }
    public void setLiquidationThresholds(List<Double> liquidationThresholds) { this.liquidationThresholds = liquidationThresholds; }
    public double getHealthyThreshold() { return healthyThreshold; }
    public void setHealthyThreshold(double healthyThreshold) { this.healthyThreshold = healthyThreshold; }
    public double getTargetHealthFactor() { return targetHealthFactor; }
    public void setTargetHealthFactor(double targetHealthFactor) { this.targetHealthFactor = targetHealthFactor; }
    public List<String> getMonitoringEventTimes() { return monitoringEventTimes; }
    public void setMonitoringEventTimes(List<String> monitoringEventTimes) { this.monitoringEventTimes = monitoringEventTimes; }
}
