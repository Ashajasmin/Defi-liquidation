package org.actus.risksrv3.models.supplychaintariff1;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

/**
 * WorkingCapitalStressModelData
 *
 * MongoDB document for WorkingCapitalStressModel configuration.
 * Collection: workingCapitalStressModels
 *
 * Domain 2 — Model 7.2: Working Capital Stress
 * Tariff escalation reduces revenue, extends collection cycles,
 * increases inventory holding periods. Monitors TARIFF_INDEX,
 * REVENUE_INDEX, DSO_INDEX to compute expected working capital gap
 * and returns drawdown fraction on revolving credit facility.
 *
 * JSON example:
 * {
 *   "riskFactorId": "wcs_ind01",
 *   "tariffIndexMOC": "TARIFF_INDEX",
 *   "revenueIndexMOC": "REVENUE_INDEX",
 *   "dsoIndexMOC": "DSO_INDEX",
 *   "baseDSO": 45.0,
 *   "baseDIO": 30.0,
 *   "baseDPO": 35.0,
 *   "tariffDSOSensitivity": 0.5,
 *   "tariffDIOSensitivity": 0.3,
 *   "maxDrawdownFraction": 1.0,
 *   "monitoringEventTimes": ["2025-06-01T00:00:00", ...]
 * }
 */
@Document(collection = "workingCapitalStressModels")
public class WorkingCapitalStressModelData {

    @Id
    private String riskFactorId;

    private String tariffIndexMOC;
    private String revenueIndexMOC;
    private String dsoIndexMOC;
    private double baseDSO;
    private double baseDIO;
    private double baseDPO;
    private double tariffDSOSensitivity;
    private double tariffDIOSensitivity;
    private double maxDrawdownFraction;
    private List<String> monitoringEventTimes;

    public WorkingCapitalStressModelData() {
    }

    public String getRiskFactorId() { return riskFactorId; }
    public void setRiskFactorId(String riskFactorId) { this.riskFactorId = riskFactorId; }

    public String getTariffIndexMOC() { return tariffIndexMOC; }
    public void setTariffIndexMOC(String tariffIndexMOC) { this.tariffIndexMOC = tariffIndexMOC; }

    public String getRevenueIndexMOC() { return revenueIndexMOC; }
    public void setRevenueIndexMOC(String revenueIndexMOC) { this.revenueIndexMOC = revenueIndexMOC; }

    public String getDsoIndexMOC() { return dsoIndexMOC; }
    public void setDsoIndexMOC(String dsoIndexMOC) { this.dsoIndexMOC = dsoIndexMOC; }

    public double getBaseDSO() { return baseDSO; }
    public void setBaseDSO(double baseDSO) { this.baseDSO = baseDSO; }

    public double getBaseDIO() { return baseDIO; }
    public void setBaseDIO(double baseDIO) { this.baseDIO = baseDIO; }

    public double getBaseDPO() { return baseDPO; }
    public void setBaseDPO(double baseDPO) { this.baseDPO = baseDPO; }

    public double getTariffDSOSensitivity() { return tariffDSOSensitivity; }
    public void setTariffDSOSensitivity(double tariffDSOSensitivity) { this.tariffDSOSensitivity = tariffDSOSensitivity; }

    public double getTariffDIOSensitivity() { return tariffDIOSensitivity; }
    public void setTariffDIOSensitivity(double tariffDIOSensitivity) { this.tariffDIOSensitivity = tariffDIOSensitivity; }

    public double getMaxDrawdownFraction() { return maxDrawdownFraction; }
    public void setMaxDrawdownFraction(double maxDrawdownFraction) { this.maxDrawdownFraction = maxDrawdownFraction; }

    public List<String> getMonitoringEventTimes() { return monitoringEventTimes; }
    public void setMonitoringEventTimes(List<String> monitoringEventTimes) { this.monitoringEventTimes = monitoringEventTimes; }
}
