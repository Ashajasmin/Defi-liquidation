package org.actus.risksrv3.utils.hybridtreasury1;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.hybridtreasury1.IntegratedStressModelData;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;
import org.actus.risksrv3.utils.MultiMarketRiskModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * IntegratedStressModel (Domain 4 — Treasury Model 5.8)
 *
 * Runs at portfolio level to monitor composite stress across all
 * domains. Loads a composite stress scenario index and amplifies
 * the signal when cascading effects are detected.
 *
 * Unlike the other 7 treasury models which each focus on one risk
 * dimension, this model provides a holistic portfolio stress signal.
 * All behavioral models run during the same simulation pass; this
 * model reads the composite stress index to provide an aggregated
 * portfolio-level intervention signal.
 *
 * Decision logic at each check date:
 *   compositeStress = COMPOSITE_STRESS_INDEX from marketModel
 *
 *   compositeStress < lowThreshold    → return 0.0 (normal)
 *   compositeStress >= lowThreshold   → return scaled fraction
 *     fraction = (compositeStress - lowThreshold) / (1.0 - lowThreshold)
 *   compositeStress >= highThreshold  → return cascadeMultiplier × fraction
 *     (cascade amplification when multiple stressors compound)
 *
 * ACTUS contract type: Any (portfolio-level monitor)
 * Market Object Codes consumed:
 *   COMPOSITE_STRESS_INDEX — aggregate stress indicator (0.0–1.0)
 */
public class IntegratedStressModel implements BehaviorRiskModelProvider {

    public static final String CALLOUT_TYPE = "MRD";

    private final String riskFactorId;
    private final String compositeStressIndexMOC;
    private final double lowThreshold;
    private final double highThreshold;
    private final double cascadeMultiplier;
    private final List<String> monitoringEventTimes;
    private final MultiMarketRiskModel marketModel;

    public IntegratedStressModel(String riskFactorId,
                                 IntegratedStressModelData data,
                                 MultiMarketRiskModel marketModel) {
        this.riskFactorId            = riskFactorId;
        this.compositeStressIndexMOC = data.getCompositeStressIndexMOC();
        this.lowThreshold            = data.getLowThreshold();
        this.highThreshold           = data.getHighThreshold();
        this.cascadeMultiplier       = data.getCascadeMultiplier();
        this.monitoringEventTimes    = data.getMonitoringEventTimes();
        this.marketModel             = marketModel;
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
            if (ied != null) { LocalDateTime edt = LocalDateTime.parse(eventTime); if (edt.isBefore(ied)) { System.out.println("**** IntegratedStressModel: SKIPPING pre-IED callout " + eventTime + " (IED=" + ied + ")"); continue; } }
            callouts.add(new CalloutData(this.riskFactorId, eventTime, CALLOUT_TYPE));
        }
        return callouts;
    }

    @Override
    public double stateAt(String id, LocalDateTime time, StateSpace states) {

        double compositeStress = this.marketModel.stateAt(this.compositeStressIndexMOC, time);

        System.out.println("**** IntegratedStressModel: time=" + time
                + " compositeStress=" + String.format("%.4f", compositeStress)
                + " lowThreshold=" + lowThreshold
                + " highThreshold=" + highThreshold
                + " cascadeMultiplier=" + cascadeMultiplier);

        if (compositeStress < this.lowThreshold) {
            return 0.0;
        }

        double baseFraction = (compositeStress - this.lowThreshold)
                / (1.0 - this.lowThreshold);

        if (compositeStress >= this.highThreshold) {
            double cascadeFraction = baseFraction * this.cascadeMultiplier;
            System.out.println("**** IntegratedStressModel: CASCADE_STRESS"
                    + " base=" + String.format("%.4f", baseFraction)
                    + " amplified=" + String.format("%.4f", cascadeFraction)
                    + " → portfolio-level intervention");
            return Math.min(1.0, cascadeFraction);
        } else {
            System.out.println("**** IntegratedStressModel: ELEVATED_STRESS"
                    + " fraction=" + String.format("%.4f", baseFraction)
                    + " → monitoring");
            return Math.min(1.0, baseFraction);
        }
    }
}
