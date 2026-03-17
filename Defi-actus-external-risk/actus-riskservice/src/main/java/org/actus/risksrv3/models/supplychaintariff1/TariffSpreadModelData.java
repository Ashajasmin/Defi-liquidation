package org.actus.risksrv3.models.supplychaintariff1;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

/**
 * TariffSpreadModelData
 *
 * MongoDB document for TariffSpreadModel configuration.
 * Collection: tariffSpreadModels
 *
 * Domain 2 — Model 7.1: Tariff-Adjusted Credit Spread
 * References: Hertel (1997), Armington (1969), GTAP Database v11
 *
 * At each rate reset (RR event), looks up TARIFF_INDEX from marketModel.
 * Computes: spreadAdjustment = baseTariffSensitivity × (currentTariffIndex - baseTariffIndex)
 * Returns adjusted rate that actus-core applies at the next RR event.
 *
 * JSON example:
 * {
 *   "riskFactorId": "ts_ind01",
 *   "tariffIndexMOC": "TARIFF_INDEX",
 *   "baseSpread": 0.02,
 *   "baseTariffIndex": 0.10,
 *   "baseTariffSensitivity": 0.5,
 *   "maxSpreadCap": 0.08,
 *   "armingtonElasticity": 2.8,
 *   "monitoringEventTimes": ["2025-06-01T00:00:00", ...]
 * }
 */
@Document(collection = "tariffSpreadModels")
public class TariffSpreadModelData {

    @Id
    private String riskFactorId;

    private String tariffIndexMOC;
    private double baseSpread;
    private double baseTariffIndex;
    private double baseTariffSensitivity;
    private double maxSpreadCap;
    private double armingtonElasticity;
    private List<String> monitoringEventTimes;

    public TariffSpreadModelData() {
    }

    public String getRiskFactorId() { return riskFactorId; }
    public void setRiskFactorId(String riskFactorId) { this.riskFactorId = riskFactorId; }

    public String getTariffIndexMOC() { return tariffIndexMOC; }
    public void setTariffIndexMOC(String tariffIndexMOC) { this.tariffIndexMOC = tariffIndexMOC; }

    public double getBaseSpread() { return baseSpread; }
    public void setBaseSpread(double baseSpread) { this.baseSpread = baseSpread; }

    public double getBaseTariffIndex() { return baseTariffIndex; }
    public void setBaseTariffIndex(double baseTariffIndex) { this.baseTariffIndex = baseTariffIndex; }

    public double getBaseTariffSensitivity() { return baseTariffSensitivity; }
    public void setBaseTariffSensitivity(double baseTariffSensitivity) { this.baseTariffSensitivity = baseTariffSensitivity; }

    public double getMaxSpreadCap() { return maxSpreadCap; }
    public void setMaxSpreadCap(double maxSpreadCap) { this.maxSpreadCap = maxSpreadCap; }

    public double getArmingtonElasticity() { return armingtonElasticity; }
    public void setArmingtonElasticity(double armingtonElasticity) { this.armingtonElasticity = armingtonElasticity; }

    public List<String> getMonitoringEventTimes() { return monitoringEventTimes; }
    public void setMonitoringEventTimes(List<String> monitoringEventTimes) { this.monitoringEventTimes = monitoringEventTimes; }
}
