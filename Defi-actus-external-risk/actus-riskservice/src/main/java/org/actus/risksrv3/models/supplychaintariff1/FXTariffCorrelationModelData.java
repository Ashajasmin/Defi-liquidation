package org.actus.risksrv3.models.supplychaintariff1;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

/**
 * FXTariffCorrelationModelData
 *
 * MongoDB document for FXTariffCorrelationModel configuration.
 * Collection: fxTariffCorrelationModels
 *
 * Domain 2 — Model 7.5: FX-Tariff Correlation
 * References: BIS Bulletin No. 105 (2025), CME Group FX hedging research
 *
 * Tariff announcements often trigger FX moves (e.g. CNY/INR devaluation).
 * Combined effect worse than sum of individual impacts.
 * Monitors TARIFF_INDEX and USD_INR (or USD_CNY) simultaneously.
 * Computes correlation-adjusted stress impact.
 *
 * JSON example:
 * {
 *   "riskFactorId": "fxtc_ind01",
 *   "tariffIndexMOC": "TARIFF_INDEX",
 *   "fxRateMOC": "USD_INR_SPOT",
 *   "baseFxRate": 83.0,
 *   "correlationCoefficient": 0.65,
 *   "fxSensitivity": 0.4,
 *   "amplificationFactor": 1.3,
 *   "monitoringEventTimes": ["2025-06-01T00:00:00", ...]
 * }
 */
@Document(collection = "fxTariffCorrelationModels")
public class FXTariffCorrelationModelData {

    @Id
    private String riskFactorId;

    private String tariffIndexMOC;
    private String fxRateMOC;
    private double baseFxRate;
    private double correlationCoefficient;
    private double fxSensitivity;
    private double amplificationFactor;
    private List<String> monitoringEventTimes;

    public FXTariffCorrelationModelData() {
    }

    public String getRiskFactorId() { return riskFactorId; }
    public void setRiskFactorId(String riskFactorId) { this.riskFactorId = riskFactorId; }

    public String getTariffIndexMOC() { return tariffIndexMOC; }
    public void setTariffIndexMOC(String tariffIndexMOC) { this.tariffIndexMOC = tariffIndexMOC; }

    public String getFxRateMOC() { return fxRateMOC; }
    public void setFxRateMOC(String fxRateMOC) { this.fxRateMOC = fxRateMOC; }

    public double getBaseFxRate() { return baseFxRate; }
    public void setBaseFxRate(double baseFxRate) { this.baseFxRate = baseFxRate; }

    public double getCorrelationCoefficient() { return correlationCoefficient; }
    public void setCorrelationCoefficient(double correlationCoefficient) { this.correlationCoefficient = correlationCoefficient; }

    public double getFxSensitivity() { return fxSensitivity; }
    public void setFxSensitivity(double fxSensitivity) { this.fxSensitivity = fxSensitivity; }

    public double getAmplificationFactor() { return amplificationFactor; }
    public void setAmplificationFactor(double amplificationFactor) { this.amplificationFactor = amplificationFactor; }

    public List<String> getMonitoringEventTimes() { return monitoringEventTimes; }
    public void setMonitoringEventTimes(List<String> monitoringEventTimes) { this.monitoringEventTimes = monitoringEventTimes; }
}
