package org.actus.risksrv3.models.stablecoin;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

/**
 * RedemptionPressureModelData
 *
 * MongoDB document for RedemptionPressureModel configuration.
 * Collection: redemptionPressureModels
 *
 * JSON example:
 * {
 *   "riskFactorId": "rp_sc01",
 *   "pegDeviationMOC": "STABLECOIN_PEG_DEV",
 *   "cashReserveMOC": "SC_CASH_RESERVE",
 *   "pegDeviationThreshold": 0.005,
 *   "monitoringEventTimes": ["2025-06-01T00:00:00", ...]
 * }
 */
@Document(collection = "redemptionPressureModels")
public class RedemptionPressureModelData {

    @Id
    private String riskFactorId;

    private String pegDeviationMOC;
    private String cashReserveMOC;
    private double pegDeviationThreshold;
    private List<String> monitoringEventTimes;

    public RedemptionPressureModelData() {
    }

    public String getRiskFactorId() {
        return riskFactorId;
    }

    public void setRiskFactorId(String riskFactorId) {
        this.riskFactorId = riskFactorId;
    }

    public String getPegDeviationMOC() {
        return pegDeviationMOC;
    }

    public void setPegDeviationMOC(String pegDeviationMOC) {
        this.pegDeviationMOC = pegDeviationMOC;
    }

    public String getCashReserveMOC() {
        return cashReserveMOC;
    }

    public void setCashReserveMOC(String cashReserveMOC) {
        this.cashReserveMOC = cashReserveMOC;
    }

    public double getPegDeviationThreshold() {
        return pegDeviationThreshold;
    }

    public void setPegDeviationThreshold(double pegDeviationThreshold) {
        this.pegDeviationThreshold = pegDeviationThreshold;
    }

    public List<String> getMonitoringEventTimes() {
        return monitoringEventTimes;
    }

    public void setMonitoringEventTimes(List<String> monitoringEventTimes) {
        this.monitoringEventTimes = monitoringEventTimes;
    }
}
