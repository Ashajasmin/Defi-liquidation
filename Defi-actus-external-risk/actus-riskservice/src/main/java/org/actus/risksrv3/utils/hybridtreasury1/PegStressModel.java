package org.actus.risksrv3.utils.hybridtreasury1;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.hybridtreasury1.PegStressModelData;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;
import org.actus.risksrv3.utils.MultiMarketRiskModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * PegStressModel (Domain 4 — Treasury Model 5.3)
 *
 * Monitors stablecoin peg deviations within the treasury stablecoin
 * allocation. When one stablecoin de-pegs but others remain stable,
 * signals redistribution. If all stressed simultaneously, signals
 * shift to fiat.
 *
 * Accepts EITHER peg rates (e.g. 0.9985) or deviation values (e.g. -0.0015).
 * If the absolute value > 0.5 it is interpreted as a peg rate and deviation
 * is computed as |1.0 - pegRate|. Otherwise the value is treated as a
 * deviation directly.
 *
 * ACTUS contract type: PAM/CLM (stablecoin position)
 * Market Object Codes consumed:
 *   USDC_USD_PEG (or DEVIATION)  — primary stablecoin peg rate / deviation
 *   USDT_USD_PEG (or DEVIATION)  — alternative stablecoin peg rate / deviation
 */
public class PegStressModel implements BehaviorRiskModelProvider {

    public static final String CALLOUT_TYPE = "MRD";

    private final String riskFactorId;
    private final String primaryPegDeviationMOC;
    private final String altPegDeviationMOC;
    private final double pegDeviationThreshold;
    private final double criticalDeviation;
    private final List<String> monitoringEventTimes;
    private final MultiMarketRiskModel marketModel;

    public PegStressModel(String riskFactorId,
                          PegStressModelData data,
                          MultiMarketRiskModel marketModel) {
        this.riskFactorId           = riskFactorId;
        this.primaryPegDeviationMOC = data.getPrimaryPegDeviationMOC();
        this.altPegDeviationMOC     = data.getAltPegDeviationMOC();
        this.pegDeviationThreshold  = data.getPegDeviationThreshold();
        this.criticalDeviation      = data.getCriticalDeviation();
        this.monitoringEventTimes   = data.getMonitoringEventTimes();
        this.marketModel            = marketModel;
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
            if (ied != null) { LocalDateTime edt = LocalDateTime.parse(eventTime); if (edt.isBefore(ied)) { System.out.println("**** PegStressModel: SKIPPING pre-IED callout " + eventTime + " (IED=" + ied + ")"); continue; } }
            callouts.add(new CalloutData(this.riskFactorId, eventTime, CALLOUT_TYPE));
        }
        return callouts;
    }

    @Override
    public double stateAt(String id, LocalDateTime time, StateSpace states) {

        double primaryRaw = this.marketModel.stateAt(this.primaryPegDeviationMOC, time);
        double altRaw     = this.marketModel.stateAt(this.altPegDeviationMOC, time);

        // Auto-detect: if |value| > 0.5 it is a peg rate → compute deviation from 1.0
        // Otherwise treat as a deviation value directly
        double primaryDev = Math.abs(primaryRaw) > 0.5
                ? Math.abs(1.0 - primaryRaw)
                : Math.abs(primaryRaw);
        double altDev = Math.abs(altRaw) > 0.5
                ? Math.abs(1.0 - altRaw)
                : Math.abs(altRaw);

        double holding = Math.abs(states.notionalPrincipal);

        System.out.println("**** PegStressModel: time=" + time
                + " primaryRaw=" + String.format("%.6f", primaryRaw)
                + " primaryDev=" + String.format("%.6f", primaryDev)
                + " altRaw=" + String.format("%.6f", altRaw)
                + " altDev=" + String.format("%.6f", altDev)
                + " holding=" + String.format("%.2f", holding)
                + " threshold=" + pegDeviationThreshold
                + " critical=" + criticalDeviation);

        if (primaryDev < this.pegDeviationThreshold) {
            return 0.0;
        }

        if (primaryDev >= this.criticalDeviation) {
            double severityFraction = Math.min(1.0,
                    primaryDev / (this.criticalDeviation * 2.0));
            System.out.println("**** PegStressModel: CRITICAL deviation="
                    + String.format("%.4f", primaryDev)
                    + " → shift to fiat, fraction="
                    + String.format("%.4f", severityFraction));
            return severityFraction;
        }

        if (altDev < this.pegDeviationThreshold) {
            double redistributeFraction = (primaryDev - this.pegDeviationThreshold)
                    / (this.criticalDeviation - this.pegDeviationThreshold);
            redistributeFraction = Math.min(1.0, Math.max(0.0, redistributeFraction));
            System.out.println("**** PegStressModel: PRIMARY_DEPEG, ALT_STABLE"
                    + " → redistribute fraction="
                    + String.format("%.4f", redistributeFraction));
            return redistributeFraction * 0.5;
        } else {
            double systemicFraction = Math.max(primaryDev, altDev)
                    / this.criticalDeviation;
            systemicFraction = Math.min(1.0, systemicFraction);
            System.out.println("**** PegStressModel: SYSTEMIC_STRESS"
                    + " both deviating → shift to fiat, fraction="
                    + String.format("%.4f", systemicFraction));
            return systemicFraction;
        }
    }
}
