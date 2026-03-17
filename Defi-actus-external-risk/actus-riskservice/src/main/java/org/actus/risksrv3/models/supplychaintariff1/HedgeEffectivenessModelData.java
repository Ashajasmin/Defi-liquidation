package org.actus.risksrv3.models.supplychaintariff1;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

/**
 * HedgeEffectivenessModelData
 *
 * MongoDB document for HedgeEffectivenessModel configuration.
 * Collection: hedgeEffectivenessModels
 *
 * Domain 2 — Model 7.3: Hedge Effectiveness
 * References: ASC 815 (FASB), IAS 39 / IFRS 9
 *
 * Runs on FX forward/swap contracts. Compares hedged notional against
 * current exposure. Returns hedge ratio. When outside 80%-125% band
 * (ASC 815), flags hedge accounting risk.
 *
 * JSON example:
 * {
 *   "riskFactorId": "he_ind01",
 *   "tariffIndexMOC": "TARIFF_INDEX",
 *   "fxRateMOC": "USD_INR_SPOT",
 *   "hedgedNotional": 5000000.0,
 *   "currentExposureMOC": "EXPORT_REVENUE_USD",
 *   "lowerEffectivenessBound": 0.80,
 *   "upperEffectivenessBound": 1.25,
 *   "tariffExposureSensitivity": 0.3,
 *   "monitoringEventTimes": ["2025-06-01T00:00:00", ...]
 * }
 */
@Document(collection = "hedgeEffectivenessModels")
public class HedgeEffectivenessModelData {

    @Id
    private String riskFactorId;

    private String tariffIndexMOC;
    private String fxRateMOC;
    private double hedgedNotional;
    private String currentExposureMOC;
    private double lowerEffectivenessBound;
    private double upperEffectivenessBound;
    private double tariffExposureSensitivity;
    private List<String> monitoringEventTimes;

    public HedgeEffectivenessModelData() {
    }

    public String getRiskFactorId() { return riskFactorId; }
    public void setRiskFactorId(String riskFactorId) { this.riskFactorId = riskFactorId; }

    public String getTariffIndexMOC() { return tariffIndexMOC; }
    public void setTariffIndexMOC(String tariffIndexMOC) { this.tariffIndexMOC = tariffIndexMOC; }

    public String getFxRateMOC() { return fxRateMOC; }
    public void setFxRateMOC(String fxRateMOC) { this.fxRateMOC = fxRateMOC; }

    public double getHedgedNotional() { return hedgedNotional; }
    public void setHedgedNotional(double hedgedNotional) { this.hedgedNotional = hedgedNotional; }

    public String getCurrentExposureMOC() { return currentExposureMOC; }
    public void setCurrentExposureMOC(String currentExposureMOC) { this.currentExposureMOC = currentExposureMOC; }

    public double getLowerEffectivenessBound() { return lowerEffectivenessBound; }
    public void setLowerEffectivenessBound(double lowerEffectivenessBound) { this.lowerEffectivenessBound = lowerEffectivenessBound; }

    public double getUpperEffectivenessBound() { return upperEffectivenessBound; }
    public void setUpperEffectivenessBound(double upperEffectivenessBound) { this.upperEffectivenessBound = upperEffectivenessBound; }

    public double getTariffExposureSensitivity() { return tariffExposureSensitivity; }
    public void setTariffExposureSensitivity(double tariffExposureSensitivity) { this.tariffExposureSensitivity = tariffExposureSensitivity; }

    public List<String> getMonitoringEventTimes() { return monitoringEventTimes; }
    public void setMonitoringEventTimes(List<String> monitoringEventTimes) { this.monitoringEventTimes = monitoringEventTimes; }
}
