package org.actus.risksrv3.utils.hybridtreasury1;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.hybridtreasury1.ScheduledCashFlowModelData;
import org.actus.risksrv3.models.hybridtreasury1.ScheduledCashFlowModelData.CashFlowEntry;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ScheduledCashFlowModel (Domain 4 — Treasury Model 5.9)
 *
 * Routes pre-configured, deterministic USD cash flows into CASH-PAM as PP
 * signals, enabling CASH-PAM to function as a central cash pool ledger.
 *
 * Use cases:
 *   - T-Bill maturity inflows   (amountUSD > 0, e.g. +$500K at TBILL maturity)
 *   - T-Bill purchase outflows  (amountUSD < 0, e.g. -$250K at TBILL IED)
 *   - CLM interest inflows      (amountUSD > 0, approximate monthly IP)
 *   - CLM maturity inflows      (amountUSD > 0, final NP + BTC appreciation)
 *
 * Signal computation:
 *   signal = amountUSD / abs(currentNP)
 *
 * This ensures:
 *   POF_PP_rf2 payoff = roleSign × signal × NP
 *                     = 1 × (amountUSD/NP) × NP
 *                     = amountUSD   (correct sign for inflow/outflow)
 *
 * STF_PP_rf2 NP update:
 *   NP -= signal × NP = NP - amountUSD
 *   → inflow:  NP decreases (capital recovered into PAM vehicle)
 *   → outflow: NP increases (capital deployed from PAM vehicle)
 *
 * Attached to: CASH-PAM-3M (or any PAM-type treasury vehicle in RPA role)
 * Callout type: MRD (maps to PP events in actus-service)
 *
 * IMPORTANT: monitoringEventTimes must include ONLY dates present in
 * cashFlowSchedule. Any monitoring time not in the schedule returns 0.0
 * with a diagnostic log, generating a zero-payoff PP event (harmless).
 */
public class ScheduledCashFlowModel implements BehaviorRiskModelProvider {

    public static final String CALLOUT_TYPE = "MRD";

    private final String riskFactorId;
    private final Map<LocalDateTime, Double> schedule; // time → amountUSD
    private final List<String> monitoringEventTimes;

    public ScheduledCashFlowModel(String riskFactorId,
                                  ScheduledCashFlowModelData data) {
        this.riskFactorId = riskFactorId;
        this.monitoringEventTimes = data.getMonitoringEventTimes();

        // Build LocalDateTime → amountUSD lookup from CashFlowEntry list
        this.schedule = new HashMap<>();
        if (data.getCashFlowSchedule() != null) {
            for (CashFlowEntry entry : data.getCashFlowSchedule()) {
                LocalDateTime t = LocalDateTime.parse(entry.getTime());
                this.schedule.put(t, entry.getAmountUSD());
            }
        }
    }

    @Override
    public Set<String> keys() {
        Set<String> keys = new HashSet<>();
        keys.add(this.riskFactorId);
        return keys;
    }

    @Override
    public List<CalloutData> contractStart(ContractModel contract) {
        LocalDateTime ied = contract.getAs("initialExchangeDate");
        List<CalloutData> callouts = new ArrayList<>();
        for (String eventTime : this.monitoringEventTimes) {
            if (ied != null) {
                LocalDateTime eventDateTime = LocalDateTime.parse(eventTime);
                if (eventDateTime.isBefore(ied)) {
                    System.out.println("**** ScheduledCashFlowModel [" + this.riskFactorId
                            + "]: SKIPPING pre-IED callout " + eventTime + " (IED=" + ied + ")");
                    continue;
                }
            }
            callouts.add(new CalloutData(this.riskFactorId, eventTime, CALLOUT_TYPE));
        }
        return callouts;
    }

    @Override
    public double stateAt(String id, LocalDateTime time, StateSpace states) {

        Double amountUSD = this.schedule.get(time);

        if (amountUSD == null) {
            System.out.println("**** ScheduledCashFlowModel [" + this.riskFactorId
                    + "]: time=" + time + " → no scheduled flow, signal=0.0");
            return 0.0;
        }

        if (amountUSD == 0.0) {
            System.out.println("**** ScheduledCashFlowModel [" + this.riskFactorId
                    + "]: time=" + time + " amountUSD=0.0 → signal=0.0");
            return 0.0;
        }

        double currentNP = Math.abs(states.notionalPrincipal);

        if (currentNP <= 0.0) {
            System.out.println("**** ScheduledCashFlowModel [" + this.riskFactorId
                    + "]: time=" + time + " WARNING currentNP<=0, cannot compute signal → 0.0");
            return 0.0;
        }

        // signal = amountUSD / NP
        // POF_PP_rf2 payoff = roleSign(RPA=1) × signal × NP = amountUSD
        double signal = amountUSD / currentNP;

        String direction = amountUSD > 0 ? "INFLOW +" : "OUTFLOW ";
        System.out.println("**** ScheduledCashFlowModel [" + this.riskFactorId
                + "]: time=" + time
                + " " + direction + String.format("%.2f", amountUSD)
                + " currentNP=" + String.format("%.2f", currentNP)
                + " → signal=" + String.format("%.8f", signal));

        return signal;
    }
}
