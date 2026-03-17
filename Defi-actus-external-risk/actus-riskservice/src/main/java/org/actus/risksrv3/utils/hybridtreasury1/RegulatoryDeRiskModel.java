package org.actus.risksrv3.utils.hybridtreasury1;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.hybridtreasury1.RegulatoryDeRiskModelData;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;
import org.actus.risksrv3.utils.MultiMarketRiskModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * RegulatoryDeRiskModel (Domain 4 — Treasury Model 5.4)
 *
 * Implements staged liquidation of digital asset positions when
 * regulatory stress exceeds threshold. Stateful — tracks cumulative
 * liquidation progress across MRD callouts.
 *
 * ACTUS contract type: STK (digital asset position)
 * Market Object Codes consumed:
 *   US_REGULATORY_STRESS — composite regulatory risk signal (0.0–1.0)
 *   MARKET_DEPTH         — market depth/liquidity indicator
 */
public class RegulatoryDeRiskModel implements BehaviorRiskModelProvider {

    public static final String CALLOUT_TYPE = "MRD";

    private final String riskFactorId;
    private final String regulatoryStressMOC;
    private final String marketDepthMOC;
    private final double regulatoryThreshold;
    private final double dailyLiquidationFraction;
    private final int maxDays;
    private final double minMarketDepth;
    private final List<String> monitoringEventTimes;
    private final MultiMarketRiskModel marketModel;

    private double cumulativeLiquidated = 0.0;
    private int liquidationDaysActive = 0;

    public RegulatoryDeRiskModel(String riskFactorId,
                                 RegulatoryDeRiskModelData data,
                                 MultiMarketRiskModel marketModel) {
        this.riskFactorId            = riskFactorId;
        this.regulatoryStressMOC     = data.getRegulatoryStressMOC();
        this.marketDepthMOC          = data.getMarketDepthMOC();
        this.regulatoryThreshold     = data.getRegulatoryThreshold();
        this.dailyLiquidationFraction = data.getDailyLiquidationFraction();
        this.maxDays                 = data.getMaxDays();
        this.minMarketDepth          = data.getMinMarketDepth();
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
            if (ied != null) { LocalDateTime edt = LocalDateTime.parse(eventTime); if (edt.isBefore(ied)) { System.out.println("**** RegulatoryDeRiskModel: SKIPPING pre-IED callout " + eventTime + " (IED=" + ied + ")"); continue; } }
            callouts.add(new CalloutData(this.riskFactorId, eventTime, CALLOUT_TYPE));
        }
        return callouts;
    }

    @Override
    public double stateAt(String id, LocalDateTime time, StateSpace states) {

        double regStress = this.marketModel.stateAt(this.regulatoryStressMOC, time);
        double marketDepth = this.marketModel.stateAt(this.marketDepthMOC, time);
        double remainingPosition = Math.abs(states.notionalPrincipal);

        System.out.println("**** RegulatoryDeRiskModel: time=" + time
                + " regStress=" + String.format("%.4f", regStress)
                + " marketDepth=" + String.format("%.4f", marketDepth)
                + " remainingPosition=" + String.format("%.2f", remainingPosition)
                + " cumulativeLiquidated=" + String.format("%.4f", cumulativeLiquidated)
                + " liquidationDaysActive=" + liquidationDaysActive);

        if (regStress < this.regulatoryThreshold) {
            if (this.liquidationDaysActive > 0) {
                System.out.println("**** RegulatoryDeRiskModel: Stress subsided ("
                        + String.format("%.4f", regStress) + " < " + regulatoryThreshold
                        + ") — stopping staged liquidation after "
                        + liquidationDaysActive + " days");
                this.liquidationDaysActive = 0;
                this.cumulativeLiquidated = 0.0;
            }
            return 0.0;
        }

        if (this.cumulativeLiquidated >= 1.0 || this.liquidationDaysActive >= this.maxDays) {
            System.out.println("**** RegulatoryDeRiskModel: Max liquidation reached"
                    + " (days=" + liquidationDaysActive + "/" + maxDays
                    + " cumulative=" + String.format("%.4f", cumulativeLiquidated) + ")");
            return 0.0;
        }

        double todayFraction = this.dailyLiquidationFraction;

        if (marketDepth < this.minMarketDepth && this.minMarketDepth > 0.0) {
            double depthAdjust = marketDepth / this.minMarketDepth;
            todayFraction = todayFraction * depthAdjust;
            System.out.println("**** RegulatoryDeRiskModel: THIN_MARKET depth="
                    + String.format("%.4f", marketDepth) + " < " + minMarketDepth
                    + " → reduced to " + String.format("%.4f", todayFraction));
        }

        double remaining = 1.0 - this.cumulativeLiquidated;
        todayFraction = Math.min(todayFraction, remaining);

        this.cumulativeLiquidated += todayFraction;
        this.liquidationDaysActive++;

        System.out.println("**** RegulatoryDeRiskModel: LIQUIDATION day="
                + liquidationDaysActive + "/" + maxDays
                + " todayFraction=" + String.format("%.4f", todayFraction)
                + " cumulative=" + String.format("%.4f", cumulativeLiquidated));

        return todayFraction;
    }
}
