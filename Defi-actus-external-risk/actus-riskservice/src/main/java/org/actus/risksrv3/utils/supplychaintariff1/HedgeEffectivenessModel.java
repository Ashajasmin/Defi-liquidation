package org.actus.risksrv3.utils.supplychaintariff1;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.supplychaintariff1.HedgeEffectivenessModelData;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;
import org.actus.risksrv3.utils.MultiMarketRiskModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * HedgeEffectivenessModel (Domain 2 — Model 7.3)
 *
 * Hedge Effectiveness for FX forward/swap contracts.
 * References: ASC 815 (FASB), IAS 39 / IFRS 9
 *
 * Compares hedged notional against current exposure (which shifts as
 * tariffs change trade volumes). Returns hedge effectiveness ratio.
 * When outside ASC 815 80%-125% band, signals hedge accounting risk.
 *
 * Tariff impact: when tariffs rise, export volumes may drop,
 * reducing actual FX exposure below the hedged notional →
 * over-hedged position → hedge accounting disqualification.
 *
 * Returns: hedge ineffectiveness (0.0 = perfect, positive = over-hedged,
 *          negative = under-hedged)
 */
public class HedgeEffectivenessModel implements BehaviorRiskModelProvider {

    public static final String CALLOUT_TYPE = "MRD";

    private final String riskFactorId;
    private final String tariffIndexMOC;
    private final String fxRateMOC;
    private final double hedgedNotional;
    private final String currentExposureMOC;
    private final double lowerEffectivenessBound;
    private final double upperEffectivenessBound;
    private final double tariffExposureSensitivity;
    private final List<String> monitoringEventTimes;
    private final MultiMarketRiskModel marketModel;

    public HedgeEffectivenessModel(String riskFactorId,
                                   HedgeEffectivenessModelData data,
                                   MultiMarketRiskModel marketModel) {
        this.riskFactorId             = riskFactorId;
        this.tariffIndexMOC           = data.getTariffIndexMOC();
        this.fxRateMOC                = data.getFxRateMOC();
        this.hedgedNotional           = data.getHedgedNotional();
        this.currentExposureMOC       = data.getCurrentExposureMOC();
        this.lowerEffectivenessBound  = data.getLowerEffectivenessBound();
        this.upperEffectivenessBound  = data.getUpperEffectivenessBound();
        this.tariffExposureSensitivity = data.getTariffExposureSensitivity();
        this.monitoringEventTimes     = data.getMonitoringEventTimes();
        this.marketModel              = marketModel;
    }

    @Override
    public Set<String> keys() {
        Set<String> keys = new HashSet<>();
        keys.add(this.riskFactorId);
        return keys;
    }

    @Override
    public List<CalloutData> contractStart(ContractModel contract) {
        // PP-before-IED fix: filter out callouts before contract starts
        LocalDateTime ied = contract.getAs("initialExchangeDate");
        List<CalloutData> callouts = new ArrayList<>();
        for (String eventTime : this.monitoringEventTimes) {
            if (ied != null) {
                LocalDateTime eventDateTime = LocalDateTime.parse(eventTime);
                if (eventDateTime.isBefore(ied)) {
                    System.out.println("**** HedgeEffectivenessModel: SKIPPING pre-IED callout " + eventTime + " (IED=" + ied + ")");
                    continue;
                }
            }
            callouts.add(new CalloutData(this.riskFactorId, eventTime, CALLOUT_TYPE));
        }
        return callouts;
    }

    /**
     * Compute hedge ineffectiveness.
     *
     * @return hedge ineffectiveness ratio (0.0 = within ASC 815 bounds)
     */
    @Override
    public double stateAt(String id, LocalDateTime time, StateSpace states) {

        double currentTariff = this.marketModel.stateAt(this.tariffIndexMOC, time);
        double currentExposure = this.marketModel.stateAt(this.currentExposureMOC, time);

        // Tariff reduces actual exposure (volume effect)
        double tariffAdjustedExposure = currentExposure * (1.0 - this.tariffExposureSensitivity * currentTariff);
        tariffAdjustedExposure = Math.max(0.0, tariffAdjustedExposure);

        // Hedge ratio = hedged notional / actual exposure
        double hedgeRatio = (tariffAdjustedExposure > 0)
                ? this.hedgedNotional / tariffAdjustedExposure
                : 999.0; // infinite over-hedge if no exposure

        // ASC 815: effective if hedge ratio is between 0.80 and 1.25
        double ineffectiveness = 0.0;
        if (hedgeRatio > this.upperEffectivenessBound) {
            // Over-hedged: positive ineffectiveness
            ineffectiveness = hedgeRatio - this.upperEffectivenessBound;
        } else if (hedgeRatio < this.lowerEffectivenessBound) {
            // Under-hedged: negative ineffectiveness
            ineffectiveness = hedgeRatio - this.lowerEffectivenessBound;
        }

        System.out.println("**** HedgeEffectivenessModel: time=" + time
                + " tariff=" + String.format("%.4f", currentTariff)
                + " exposure=" + String.format("%.0f→%.0f", currentExposure, tariffAdjustedExposure)
                + " hedgeRatio=" + String.format("%.3f", hedgeRatio)
                + " ineffectiveness=" + String.format("%.4f", ineffectiveness));

        return ineffectiveness;
    }
}
