package org.actus.risksrv3.utils.supplychaintariff1;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.supplychaintariff1.TariffSpreadModelData;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;
import org.actus.risksrv3.utils.MultiMarketRiskModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * TariffSpreadModel (Domain 2 — Model 7.1)
 *
 * Tariff-Adjusted Credit Spread for exporter loans.
 * References: Hertel (1997), Armington (1969), GTAP Database v11
 *
 * At each monitoring event, looks up TARIFF_INDEX from marketModel.
 * Computes:
 *   spreadAdjustment = baseTariffSensitivity × (currentTariffIndex - baseTariffIndex)
 *
 * The Armington elasticity is used as a scaling factor:
 *   when elasticity is high (e.g. textiles=2.8), tariff pass-through to
 *   credit spreads is amplified because substitution is easier.
 *   adjustedSensitivity = baseTariffSensitivity × (armingtonElasticity / 2.0)
 *
 * Returns: spread adjustment (positive = wider spreads = higher cost)
 * Capped at maxSpreadCap to prevent unrealistic values.
 */
public class TariffSpreadModel implements BehaviorRiskModelProvider {

    public static final String CALLOUT_TYPE = "MRD";

    private final String riskFactorId;
    private final String tariffIndexMOC;
    private final double baseSpread;
    private final double baseTariffIndex;
    private final double baseTariffSensitivity;
    private final double maxSpreadCap;
    private final double armingtonElasticity;
    private final List<String> monitoringEventTimes;
    private final MultiMarketRiskModel marketModel;

    public TariffSpreadModel(String riskFactorId,
                             TariffSpreadModelData data,
                             MultiMarketRiskModel marketModel) {
        this.riskFactorId          = riskFactorId;
        this.tariffIndexMOC        = data.getTariffIndexMOC();
        this.baseSpread            = data.getBaseSpread();
        this.baseTariffIndex       = data.getBaseTariffIndex();
        this.baseTariffSensitivity = data.getBaseTariffSensitivity();
        this.maxSpreadCap          = data.getMaxSpreadCap();
        this.armingtonElasticity   = data.getArmingtonElasticity();
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
                    System.out.println("**** TariffSpreadModel: SKIPPING pre-IED callout " + eventTime + " (IED=" + ied + ")");
                    continue;
                }
            }
            callouts.add(new CalloutData(this.riskFactorId, eventTime, CALLOUT_TYPE));
        }
        return callouts;
    }

    /**
     * Compute tariff-adjusted credit spread.
     *
     * @return spread adjustment (0.0 = no change, positive = wider spread)
     */
    @Override
    public double stateAt(String id, LocalDateTime time, StateSpace states) {

        double currentTariff = this.marketModel.stateAt(this.tariffIndexMOC, time);
        double tariffDelta = currentTariff - this.baseTariffIndex;

        // Armington-scaled sensitivity: higher elasticity → more spread impact
        double adjustedSensitivity = this.baseTariffSensitivity * (this.armingtonElasticity / 2.0);

        double spreadAdjustment = adjustedSensitivity * tariffDelta;

        // Floor at 0 (tariff reduction doesn't tighten spreads below base)
        spreadAdjustment = Math.max(0.0, spreadAdjustment);

        // Cap at maxSpreadCap
        spreadAdjustment = Math.min(spreadAdjustment, this.maxSpreadCap);

        System.out.println("**** TariffSpreadModel: time=" + time
                + " tariff=" + String.format("%.4f", currentTariff)
                + " delta=" + String.format("%.4f", tariffDelta)
                + " armington=" + String.format("%.1f", this.armingtonElasticity)
                + " spreadAdj=" + String.format("%.6f", spreadAdjustment));

        return spreadAdjustment;
    }
}
