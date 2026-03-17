package org.actus.risksrv3.models.hybridtreasury1;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

/**
 * ScheduledCashFlowModelData
 *
 * MongoDB document for ScheduledCashFlowModel configuration.
 * Collection: scheduledCashFlowModels
 *
 * Routes fixed, pre-configured USD cash flows into CASH-PAM as PP signals,
 * enabling CASH-PAM to function as a central cash pool ledger.
 *
 * Each entry in cashFlowSchedule has an ISO timestamp and a signed USD amount:
 *   amountUSD > 0 : inflow  (e.g. T-Bill maturity, CLM interest/maturity received)
 *   amountUSD < 0 : outflow (e.g. T-Bill purchase, CLM deployment)
 *
 * Signal returned to POF_PP_rf2:
 *   signal = amountUSD / abs(currentNP)
 *   → POF payoff = roleSign × signal × NP = amountUSD  (sign-correct cash flow)
 *
 * Postman POST /addScheduledCashFlowModel example:
 * {
 *   "riskFactorId": "scf_tbill_flows_01",
 *   "cashFlowSchedule": [
 *     { "time": "2026-03-01T00:00:00", "amountUSD": -750000 },
 *     { "time": "2026-03-31T00:00:00", "amountUSD":  500000 },
 *     { "time": "2026-05-31T00:00:00", "amountUSD":  500000 },
 *     { "time": "2026-06-30T00:00:00", "amountUSD":  250000 },
 *     { "time": "2026-07-31T00:00:00", "amountUSD":  500000 }
 *   ],
 *   "monitoringEventTimes": [
 *     "2026-03-01T00:00:00", "2026-03-31T00:00:00",
 *     "2026-05-31T00:00:00", "2026-06-30T00:00:00", "2026-07-31T00:00:00"
 *   ]
 * }
 */
@Document(collection = "scheduledCashFlowModels")
public class ScheduledCashFlowModelData {

    @Id
    private String riskFactorId;

    private List<CashFlowEntry> cashFlowSchedule;
    private List<String> monitoringEventTimes;

    public ScheduledCashFlowModelData() {}

    // ----------------------------------------------------------------
    // Inner class: one (time, amountUSD) entry in the schedule
    // ----------------------------------------------------------------
    public static class CashFlowEntry {
        /** ISO-8601 timestamp, e.g. "2026-03-31T00:00:00" */
        private String time;
        /** Signed USD amount: positive = inflow, negative = outflow */
        private double amountUSD;

        public CashFlowEntry() {}

        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }

        public double getAmountUSD() { return amountUSD; }
        public void setAmountUSD(double amountUSD) { this.amountUSD = amountUSD; }
    }

    // ----------------------------------------------------------------
    // Getters and Setters
    // ----------------------------------------------------------------

    public String getRiskFactorId() { return riskFactorId; }
    public void setRiskFactorId(String riskFactorId) { this.riskFactorId = riskFactorId; }

    public List<CashFlowEntry> getCashFlowSchedule() { return cashFlowSchedule; }
    public void setCashFlowSchedule(List<CashFlowEntry> cashFlowSchedule) {
        this.cashFlowSchedule = cashFlowSchedule;
    }

    public List<String> getMonitoringEventTimes() { return monitoringEventTimes; }
    public void setMonitoringEventTimes(List<String> monitoringEventTimes) {
        this.monitoringEventTimes = monitoringEventTimes;
    }
}
