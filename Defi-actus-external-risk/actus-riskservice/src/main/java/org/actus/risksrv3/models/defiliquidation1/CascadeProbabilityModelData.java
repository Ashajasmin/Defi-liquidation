package org.actus.risksrv3.models.defiliquidation1;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document(collection = "cascadeProbabilityModels")
public class CascadeProbabilityModelData {
    @Id
    private String riskFactorId;
    private String collateralPriceMOC;
    private String poolAggLtvMOC;
    private String marketDepthMOC;
    private double collateralQuantity;
    private double positionValueUSD;
    private double cascadeThreshold;
    private double priceImpactFactor;
    private double defensiveRepayFraction;
    private List<String> monitoringEventTimes;

    public CascadeProbabilityModelData() {}
    public String getRiskFactorId() { return riskFactorId; }
    public void setRiskFactorId(String v) { this.riskFactorId = v; }
    public String getCollateralPriceMOC() { return collateralPriceMOC; }
    public void setCollateralPriceMOC(String v) { this.collateralPriceMOC = v; }
    public String getPoolAggLtvMOC() { return poolAggLtvMOC; }
    public void setPoolAggLtvMOC(String v) { this.poolAggLtvMOC = v; }
    public String getMarketDepthMOC() { return marketDepthMOC; }
    public void setMarketDepthMOC(String v) { this.marketDepthMOC = v; }
    public double getCollateralQuantity() { return collateralQuantity; }
    public void setCollateralQuantity(double v) { this.collateralQuantity = v; }
    public double getPositionValueUSD() { return positionValueUSD; }
    public void setPositionValueUSD(double v) { this.positionValueUSD = v; }
    public double getCascadeThreshold() { return cascadeThreshold; }
    public void setCascadeThreshold(double v) { this.cascadeThreshold = v; }
    public double getPriceImpactFactor() { return priceImpactFactor; }
    public void setPriceImpactFactor(double v) { this.priceImpactFactor = v; }
    public double getDefensiveRepayFraction() { return defensiveRepayFraction; }
    public void setDefensiveRepayFraction(double v) { this.defensiveRepayFraction = v; }
    public List<String> getMonitoringEventTimes() { return monitoringEventTimes; }
    public void setMonitoringEventTimes(List<String> v) { this.monitoringEventTimes = v; }
}