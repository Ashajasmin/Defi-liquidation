package org.actus.risksrv3.models.supplychaintariff1;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

/**
 * PortCongestionModelData
 *
 * MongoDB document for PortCongestionModel configuration.
 * Collection: portCongestionModels
 *
 * Domain 2 — Model 7.6: Port Congestion
 * References: WTO Trade Monitoring Report, port dwell time statistics
 *
 * Tariff policy changes cause port congestion, extending physical
 * and financial supply chain timelines. Monitors PORT_CONGESTION_INDEX.
 * Extends expected maturity dates on trade receivable PAM contracts,
 * increasing working capital stress.
 *
 * JSON example:
 * {
 *   "riskFactorId": "pc_ind01",
 *   "portCongestionIndexMOC": "PORT_CONGESTION_INDEX",
 *   "tariffIndexMOC": "TARIFF_INDEX",
 *   "baseDwellDays": 5.0,
 *   "congestionSensitivity": 0.4,
 *   "maxDelayDays": 30.0,
 *   "financialImpactPerDay": 0.001,
 *   "monitoringEventTimes": ["2025-06-01T00:00:00", ...]
 * }
 */
@Document(collection = "portCongestionModels")
public class PortCongestionModelData {

    @Id
    private String riskFactorId;

    private String portCongestionIndexMOC;
    private String tariffIndexMOC;
    private double baseDwellDays;
    private double congestionSensitivity;
    private double maxDelayDays;
    private double financialImpactPerDay;
    private List<String> monitoringEventTimes;

    public PortCongestionModelData() {
    }

    public String getRiskFactorId() { return riskFactorId; }
    public void setRiskFactorId(String riskFactorId) { this.riskFactorId = riskFactorId; }

    public String getPortCongestionIndexMOC() { return portCongestionIndexMOC; }
    public void setPortCongestionIndexMOC(String portCongestionIndexMOC) { this.portCongestionIndexMOC = portCongestionIndexMOC; }

    public String getTariffIndexMOC() { return tariffIndexMOC; }
    public void setTariffIndexMOC(String tariffIndexMOC) { this.tariffIndexMOC = tariffIndexMOC; }

    public double getBaseDwellDays() { return baseDwellDays; }
    public void setBaseDwellDays(double baseDwellDays) { this.baseDwellDays = baseDwellDays; }

    public double getCongestionSensitivity() { return congestionSensitivity; }
    public void setCongestionSensitivity(double congestionSensitivity) { this.congestionSensitivity = congestionSensitivity; }

    public double getMaxDelayDays() { return maxDelayDays; }
    public void setMaxDelayDays(double maxDelayDays) { this.maxDelayDays = maxDelayDays; }

    public double getFinancialImpactPerDay() { return financialImpactPerDay; }
    public void setFinancialImpactPerDay(double financialImpactPerDay) { this.financialImpactPerDay = financialImpactPerDay; }

    public List<String> getMonitoringEventTimes() { return monitoringEventTimes; }
    public void setMonitoringEventTimes(List<String> monitoringEventTimes) { this.monitoringEventTimes = monitoringEventTimes; }
}
