package org.actus.risksrv3.utils.supplychaintariff1;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.supplychaintariff1.RevenueElasticityModelData;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;
import org.actus.risksrv3.utils.MultiMarketRiskModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * RevenueElasticityModel (Domain 2 — Model 7.4)
 *
 * Revenue Elasticity — product-specific tariff pass-through.
 * References: Armington (1969), GTAP Armington elasticities by sector,
 *             Corong et al. (2017)
 *
 * Not all tariff increases pass through equally to revenue.
 * Uses product-specific Armington elasticity parameters to compute
 * revenue impact from tariff changes.
 *
 * Revenue impact formula:
 *   volumeDecline = productElasticity × tariffRate × passThrough
 *   revenueMultiplier = max(revenueFloorFraction, 1.0 - volumeDecline)
 *
 * Returns: revenue decline fraction (0.0 = no decline, e.g. 0.3 = 30% decline)
 */
public class RevenueElasticityModel implements BehaviorRiskModelProvider {

    public static final String CALLOUT_TYPE = "MRD";

    private final String riskFactorId;
    private final String tariffIndexMOC;
    private final double productElasticity;
    private final double baseRevenue;
    private final double passThrough;
    private final double revenueFloorFraction;
    private final List<String> monitoringEventTimes;
    private final MultiMarketRiskModel marketModel;

    public RevenueElasticityModel(String riskFactorId,
                                  RevenueElasticityModelData data,
                                  MultiMarketRiskModel marketModel) {
        this.riskFactorId          = riskFactorId;
        this.tariffIndexMOC        = data.getTariffIndexMOC();
        this.productElasticity     = data.getProductElasticity();
        this.baseRevenue           = data.getBaseRevenue();
        this.passThrough           = data.getPassThrough();
        this.revenueFloorFraction  = data.getRevenueFloorFraction();
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
                    System.out.println("**** RevenueElasticityModel: SKIPPING pre-IED callout " + eventTime + " (IED=" + ied + ")");
                    continue;
                }
            }
            callouts.add(new CalloutData(this.riskFactorId, eventTime, CALLOUT_TYPE));
        }
        return callouts;
    }

    /**
     * Compute revenue decline fraction from tariff.
     *
     * @return revenue decline fraction (0.0–(1-revenueFloorFraction))
     */
    @Override
    public double stateAt(String id, LocalDateTime time, StateSpace states) {

        double currentTariff = this.marketModel.stateAt(this.tariffIndexMOC, time);

        // Volume decline using Armington elasticity and pass-through
        double volumeDecline = this.productElasticity * currentTariff * this.passThrough;

        // Revenue multiplier (floored)
        double revenueMultiplier = Math.max(this.revenueFloorFraction, 1.0 - volumeDecline);

        // Return decline fraction
        double declineFraction = 1.0 - revenueMultiplier;

        System.out.println("**** RevenueElasticityModel: time=" + time
                + " tariff=" + String.format("%.4f", currentTariff)
                + " elasticity=" + String.format("%.1f", this.productElasticity)
                + " passThru=" + String.format("%.2f", this.passThrough)
                + " volumeDecline=" + String.format("%.4f", volumeDecline)
                + " revDecline=" + String.format("%.4f", declineFraction));

        return declineFraction;
    }
}
