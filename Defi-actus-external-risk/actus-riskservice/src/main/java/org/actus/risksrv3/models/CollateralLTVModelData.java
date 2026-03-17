package org.actus.risksrv3.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document(collection = "collateralLTVModels")
public class CollateralLTVModelData {

    @Id
    private String riskFactorId;

    private String collateralPriceMarketObjectCode;
    private double collateralQuantity;
    private double ltvThreshold;
    private double ltvTarget;
    private double liquidationThreshold;
    private List<String> monitoringEventTimes;

    public CollateralLTVModelData() {
    }

    public String getRiskFactorId() {
        return riskFactorId;
    }
    public void setRiskFactorId(String riskFactorId) {
        this.riskFactorId = riskFactorId;
    }
    public String getCollateralPriceMarketObjectCode() {
        return collateralPriceMarketObjectCode;
    }
    public void setCollateralPriceMarketObjectCode(String collateralPriceMarketObjectCode) {
        this.collateralPriceMarketObjectCode = collateralPriceMarketObjectCode;
    }
    public double getCollateralQuantity() {
        return collateralQuantity;
    }
    public void setCollateralQuantity(double collateralQuantity) {
        this.collateralQuantity = collateralQuantity;
    }
    public double getLtvThreshold() {
        return ltvThreshold;
    }
    public void setLtvThreshold(double ltvThreshold) {
        this.ltvThreshold = ltvThreshold;
    }
    public double getLtvTarget() {
        return ltvTarget;
    }
    public void setLtvTarget(double ltvTarget) {
        this.ltvTarget = ltvTarget;
    }
    public double getLiquidationThreshold() {
        return liquidationThreshold;
    }
    public void setLiquidationThreshold(double liquidationThreshold) {
        this.liquidationThreshold = liquidationThreshold;
    }
    public List<String> getMonitoringEventTimes() {
        return monitoringEventTimes;
    }
    public void setMonitoringEventTimes(List<String> monitoringEventTimes) {
        this.monitoringEventTimes = monitoringEventTimes;
    }
}