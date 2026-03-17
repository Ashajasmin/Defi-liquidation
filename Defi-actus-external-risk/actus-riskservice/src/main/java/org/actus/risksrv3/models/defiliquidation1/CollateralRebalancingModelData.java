package org.actus.risksrv3.models.defiliquidation1;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document(collection = "collateralRebalancingModels")
public class CollateralRebalancingModelData {
    @Id
    private String riskFactorId;
    private String volatileAssetMOC;
    private String stableAssetMOC;
    private String etfFlowMOC;
    private double volatileAssetQty;
    private double stableAssetQty;
    private double invoiceValueUSD;
    private double overallLtvThreshold;
    private double liquidationThreshold;
    private double ltvTarget;
    private double etfFlowThreshold;
    private double etfSensitivity;
    private List<String> monitoringEventTimes;

    public CollateralRebalancingModelData() {}
    public String getRiskFactorId() { return riskFactorId; }
    public void setRiskFactorId(String v) { this.riskFactorId = v; }
    public String getVolatileAssetMOC() { return volatileAssetMOC; }
    public void setVolatileAssetMOC(String v) { this.volatileAssetMOC = v; }
    public String getStableAssetMOC() { return stableAssetMOC; }
    public void setStableAssetMOC(String v) { this.stableAssetMOC = v; }
    public String getEtfFlowMOC() { return etfFlowMOC; }
    public void setEtfFlowMOC(String v) { this.etfFlowMOC = v; }
    public double getVolatileAssetQty() { return volatileAssetQty; }
    public void setVolatileAssetQty(double v) { this.volatileAssetQty = v; }
    public double getStableAssetQty() { return stableAssetQty; }
    public void setStableAssetQty(double v) { this.stableAssetQty = v; }
    public double getInvoiceValueUSD() { return invoiceValueUSD; }
    public void setInvoiceValueUSD(double v) { this.invoiceValueUSD = v; }
    public double getOverallLtvThreshold() { return overallLtvThreshold; }
    public void setOverallLtvThreshold(double v) { this.overallLtvThreshold = v; }
    public double getLiquidationThreshold() { return liquidationThreshold; }
    public void setLiquidationThreshold(double v) { this.liquidationThreshold = v; }
    public double getLtvTarget() { return ltvTarget; }
    public void setLtvTarget(double v) { this.ltvTarget = v; }
    public double getEtfFlowThreshold() { return etfFlowThreshold; }
    public void setEtfFlowThreshold(double v) { this.etfFlowThreshold = v; }
    public double getEtfSensitivity() { return etfSensitivity; }
    public void setEtfSensitivity(double v) { this.etfSensitivity = v; }
    public List<String> getMonitoringEventTimes() { return monitoringEventTimes; }
    public void setMonitoringEventTimes(List<String> v) { this.monitoringEventTimes = v; }
}