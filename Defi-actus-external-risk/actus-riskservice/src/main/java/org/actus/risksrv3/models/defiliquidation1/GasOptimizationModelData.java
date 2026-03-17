package org.actus.risksrv3.models.defiliquidation1;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document(collection = "gasOptimizationModels")
public class GasOptimizationModelData {
    @Id
    private String riskFactorId;
    private String collateralPriceMOC;
    private String gasPriceMOC;
    private double collateralQuantity;
    private double gasUnitsPerTx;
    private double ltvThreshold;
    private double liquidationThreshold;
    private double ltvTarget;
    private double minBenefitUSD;
    private List<String> monitoringEventTimes;

    public GasOptimizationModelData() {}
    public String getRiskFactorId() { return riskFactorId; }
    public void setRiskFactorId(String v) { this.riskFactorId = v; }
    public String getCollateralPriceMOC() { return collateralPriceMOC; }
    public void setCollateralPriceMOC(String v) { this.collateralPriceMOC = v; }
    public String getGasPriceMOC() { return gasPriceMOC; }
    public void setGasPriceMOC(String v) { this.gasPriceMOC = v; }
    public double getCollateralQuantity() { return collateralQuantity; }
    public void setCollateralQuantity(double v) { this.collateralQuantity = v; }
    public double getGasUnitsPerTx() { return gasUnitsPerTx; }
    public void setGasUnitsPerTx(double v) { this.gasUnitsPerTx = v; }
    public double getLtvThreshold() { return ltvThreshold; }
    public void setLtvThreshold(double v) { this.ltvThreshold = v; }
    public double getLiquidationThreshold() { return liquidationThreshold; }
    public void setLiquidationThreshold(double v) { this.liquidationThreshold = v; }
    public double getLtvTarget() { return ltvTarget; }
    public void setLtvTarget(double v) { this.ltvTarget = v; }
    public double getMinBenefitUSD() { return minBenefitUSD; }
    public void setMinBenefitUSD(double v) { this.minBenefitUSD = v; }
    public List<String> getMonitoringEventTimes() { return monitoringEventTimes; }
    public void setMonitoringEventTimes(List<String> v) { this.monitoringEventTimes = v; }
}