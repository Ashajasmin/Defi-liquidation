package org.actus.risksrv3.utils.supplychaintariff1;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.supplychaintariff1.FXTariffCorrelationModelData;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;
import org.actus.risksrv3.utils.MultiMarketRiskModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * FXTariffCorrelationModel (Domain 2 — Model 7.5)
 *
 * FX-Tariff Correlation: combined FX + tariff stress.
 * References: BIS Bulletin No. 105 (2025), CME Group FX hedging research
 *
 * Tariff announcements often trigger FX moves (e.g. CNY/INR devaluation).
 * Combined effect is worse than the sum of individual impacts due to
 * non-linear correlation. India-US corridor: INR depreciates under
 * tariff stress, increasing USD-denominated debt burden.
 *
 * Combined stress formula:
 *   tariffStress = currentTariff (normalized 0-1)
 *   fxStress = |fxRate - baseFxRate| / baseFxRate
 *   individualSum = tariffStress + (fxSensitivity × fxStress)
 *   correlatedStress = individualSum × amplificationFactor × correlationCoefficient
 *
 * Returns: combined stress factor (0.0 = no stress)
 */
public class FXTariffCorrelationModel implements BehaviorRiskModelProvider {

    public static final String CALLOUT_TYPE = "MRD";

    private final String riskFactorId;
    private final String tariffIndexMOC;
    private final String fxRateMOC;
    private final double baseFxRate;
    private final double correlationCoefficient;
    private final double fxSensitivity;
    private final double amplificationFactor;
    private final List<String> monitoringEventTimes;
    private final MultiMarketRiskModel marketModel;

    public FXTariffCorrelationModel(String riskFactorId,
                                    FXTariffCorrelationModelData data,
                                    MultiMarketRiskModel marketModel) {
        this.riskFactorId          = riskFactorId;
        this.tariffIndexMOC        = data.getTariffIndexMOC();
        this.fxRateMOC             = data.getFxRateMOC();
        this.baseFxRate            = data.getBaseFxRate();
        this.correlationCoefficient = data.getCorrelationCoefficient();
        this.fxSensitivity         = data.getFxSensitivity();
        this.amplificationFactor   = data.getAmplificationFactor();
        this.monitoringEventTimes  = data.getMonitoringEventTimes();
        this.marketModel           = marketModel;
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
                    System.out.println("**** FXTariffCorrelationModel: SKIPPING pre-IED callout " + eventTime + " (IED=" + ied + ")");
                    continue;
                }
            }
            callouts.add(new CalloutData(this.riskFactorId, eventTime, CALLOUT_TYPE));
        }
        return callouts;
    }

    /**
     * Compute correlation-adjusted FX+tariff stress.
     *
     * @return combined stress factor (0.0 = no stress)
     */
    @Override
    public double stateAt(String id, LocalDateTime time, StateSpace states) {

        double currentTariff = this.marketModel.stateAt(this.tariffIndexMOC, time);
        double currentFxRate = this.marketModel.stateAt(this.fxRateMOC, time);

        // FX stress: percentage deviation from base rate
        double fxDeviation = Math.abs(currentFxRate - this.baseFxRate) / this.baseFxRate;

        // Individual stress components
        double tariffStress = currentTariff; // normalized 0-1
        double fxStress = this.fxSensitivity * fxDeviation;

        // Correlation-amplified combined stress
        // When both move together (high correlation), amplification kicks in
        double individualSum = tariffStress + fxStress;
        double correlatedStress = individualSum * this.amplificationFactor * this.correlationCoefficient;

        // Ensure non-negative
        correlatedStress = Math.max(0.0, correlatedStress);

        System.out.println("**** FXTariffCorrelationModel: time=" + time
                + " tariff=" + String.format("%.4f", currentTariff)
                + " fx=" + String.format("%.2f→%.2f", this.baseFxRate, currentFxRate)
                + " fxDev=" + String.format("%.4f", fxDeviation)
                + " corr=" + String.format("%.2f", this.correlationCoefficient)
                + " combinedStress=" + String.format("%.4f", correlatedStress));

        return correlatedStress;
    }
}
