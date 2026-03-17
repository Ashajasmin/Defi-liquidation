package org.actus.risksrv3.models.stablecoin;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

/**
 * BackingRatioModelData
 *
 * MongoDB document for BackingRatioModel configuration.
 * Collection: backingRatioModels
 *
 * JSON example for Postman POST /addBackingRatioModel:
 * {
 *   "riskFactorId": "br_sc01",
 *   "totalReservesMOC": "SC_TOTAL_RESERVES",
 *   "cashReserveMOC": "SC_CASH_RESERVE",
 *   "backingThreshold": 1.0,
 *   "liquidityThreshold": 0.35,
 *   "monitoringEventTimes": [
 *     "2025-06-01T00:00:00", "2025-06-02T00:00:00", ...
 *   ]
 * }
 */
@Document(collection = "backingRatioModels")
public class BackingRatioModelData {

    @Id
    private String riskFactorId;

    private String totalReservesMOC;
    private String cashReserveMOC;
    private double backingThreshold;
    private double liquidityThreshold;
    private List<String> monitoringEventTimes;

    public BackingRatioModelData() {
    }

    // --- Getters and Setters ---

    public String getRiskFactorId() {
        return riskFactorId;
    }

    public void setRiskFactorId(String riskFactorId) {
        this.riskFactorId = riskFactorId;
    }

    public String getTotalReservesMOC() {
        return totalReservesMOC;
    }

    public void setTotalReservesMOC(String totalReservesMOC) {
        this.totalReservesMOC = totalReservesMOC;
    }

    public String getCashReserveMOC() {
        return cashReserveMOC;
    }

    public void setCashReserveMOC(String cashReserveMOC) {
        this.cashReserveMOC = cashReserveMOC;
    }

    public double getBackingThreshold() {
        return backingThreshold;
    }

    public void setBackingThreshold(double backingThreshold) {
        this.backingThreshold = backingThreshold;
    }

    public double getLiquidityThreshold() {
        return liquidityThreshold;
    }

    public void setLiquidityThreshold(double liquidityThreshold) {
        this.liquidityThreshold = liquidityThreshold;
    }

    public List<String> getMonitoringEventTimes() {
        return monitoringEventTimes;
    }

    public void setMonitoringEventTimes(List<String> monitoringEventTimes) {
        this.monitoringEventTimes = monitoringEventTimes;
    }
}
