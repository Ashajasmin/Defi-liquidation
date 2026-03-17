package org.actus.risksrv3.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

/**
 * BufferLTVModelData
 *
 * Configuration data for the Buffer-First LTV defense strategy.
 * This model monitors collateral LTV and uses a separate buffer reserve
 * to fund prepayments, preserving the original collateral quantity.
 *
 * Circuit breaker functionality prevents excessive buffer depletion during
 * sustained price declines ("falling knife" scenarios).
 *
 * MongoDB Collection: bufferLTVModels
 */
@Document(collection = "bufferLTVModels")
public class BufferLTVModelData {

    @Id
    private String riskFactorId;

    // Collateral configuration (used for LTV TRIGGER calculation)
    private String collateralPriceMarketObjectCode;
    private double collateralQuantity;

    // Buffer configuration (used for prepayment FUNDING)
    private String bufferContractId;
    private double initialBufferQuantity;

    // LTV thresholds
    private double ltvThreshold;
    private double ltvTarget;
    private double liquidationThreshold;

    // Circuit breaker parameters
    private int maxInterventions;
    private double minBufferReserve;
    private double maxBufferUsagePerIntervention;
    private long cooldownMillis;

    // Falling knife detection
    private double fallingKnifePriceDropThreshold;
    private long fallingKnifeTimeWindowMillis;

    // Monitoring schedule
    private List<String> monitoringEventTimes;

    // Constructors
    public BufferLTVModelData() {
    }

    // Getters and Setters
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

    public String getBufferContractId() {
        return bufferContractId;
    }

    public void setBufferContractId(String bufferContractId) {
        this.bufferContractId = bufferContractId;
    }

    public double getInitialBufferQuantity() {
        return initialBufferQuantity;
    }

    public void setInitialBufferQuantity(double initialBufferQuantity) {
        this.initialBufferQuantity = initialBufferQuantity;
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

    public int getMaxInterventions() {
        return maxInterventions;
    }

    public void setMaxInterventions(int maxInterventions) {
        this.maxInterventions = maxInterventions;
    }

    public double getMinBufferReserve() {
        return minBufferReserve;
    }

    public void setMinBufferReserve(double minBufferReserve) {
        this.minBufferReserve = minBufferReserve;
    }

    public double getMaxBufferUsagePerIntervention() {
        return maxBufferUsagePerIntervention;
    }

    public void setMaxBufferUsagePerIntervention(double maxBufferUsagePerIntervention) {
        this.maxBufferUsagePerIntervention = maxBufferUsagePerIntervention;
    }

    public long getCooldownMillis() {
        return cooldownMillis;
    }

    public void setCooldownMillis(long cooldownMillis) {
        this.cooldownMillis = cooldownMillis;
    }

    public double getFallingKnifePriceDropThreshold() {
        return fallingKnifePriceDropThreshold;
    }

    public void setFallingKnifePriceDropThreshold(double fallingKnifePriceDropThreshold) {
        this.fallingKnifePriceDropThreshold = fallingKnifePriceDropThreshold;
    }

    public long getFallingKnifeTimeWindowMillis() {
        return fallingKnifeTimeWindowMillis;
    }

    public void setFallingKnifeTimeWindowMillis(long fallingKnifeTimeWindowMillis) {
        this.fallingKnifeTimeWindowMillis = fallingKnifeTimeWindowMillis;
    }

    public List<String> getMonitoringEventTimes() {
        return monitoringEventTimes;
    }

    public void setMonitoringEventTimes(List<String> monitoringEventTimes) {
        this.monitoringEventTimes = monitoringEventTimes;
    }
}
