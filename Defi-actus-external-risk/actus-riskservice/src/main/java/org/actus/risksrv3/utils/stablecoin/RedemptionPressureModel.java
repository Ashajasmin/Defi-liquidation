package org.actus.risksrv3.utils.stablecoin;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.stablecoin.RedemptionPressureModelData;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;
import org.actus.risksrv3.utils.MultiMarketRiskModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * RedemptionPressureModel (Domain 5 — Model 6.2)
 *
 * Applies empirically calibrated redemption curves from 609 historical
 * de-peg events to model token redemption pressure.
 *
 * Monitors STABLECOIN_PEG_DEV reference index. When deviation exceeds
 * threshold, looks up daily redemption rate from calibrated breakpoints:
 *
 *   De-Peg Magnitude → Daily Redemption Rate (median from 609 events):
 *     0.5% de-peg  → 2.3% daily
 *     1.0% de-peg  → 5.7% daily
 *     3.0% de-peg  → 14.2% daily
 *     5.0% de-peg  → 28.5% daily
 *    10.0% de-peg  → 52.0% daily
 *
 * Computes expected redemption volume: supply × redemptionRate.
 * Checks if cash reserves can meet demand.
 * If cash insufficient → flags liquidity gap.
 *
 * Returns redemption fraction applied as MRD to reduce outstanding supply.
 *
 * Market Object Codes consumed:
 *   STABLECOIN_PEG_DEV — peg deviation (0.0 = on peg, 0.01 = 1% de-peg)
 *   SC_CASH_RESERVE    — immediately available cash
 */
public class RedemptionPressureModel implements BehaviorRiskModelProvider {

    public static final String CALLOUT_TYPE = "MRD";

    // -------------------------------------------------------------------------
    // Calibrated breakpoints from 609 historical de-peg events
    // -------------------------------------------------------------------------
    private static final double[] DEG_PEG_BREAKPOINTS    = {0.005, 0.01, 0.03, 0.05, 0.10};
    private static final double[] REDEMPTION_RATES       = {0.023, 0.057, 0.142, 0.285, 0.52};

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final String riskFactorId;
    private final String pegDeviationMOC;
    private final String cashReserveMOC;
    private final double pegDeviationThreshold;   // minimum deviation to trigger (e.g. 0.005)
    private final List<String> monitoringEventTimes;
    private final MultiMarketRiskModel marketModel;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public RedemptionPressureModel(String riskFactorId,
                                   RedemptionPressureModelData data,
                                   MultiMarketRiskModel marketModel) {
        this.riskFactorId         = riskFactorId;
        this.pegDeviationMOC      = data.getPegDeviationMOC();
        this.cashReserveMOC       = data.getCashReserveMOC();
        this.pegDeviationThreshold = data.getPegDeviationThreshold();
        this.monitoringEventTimes = data.getMonitoringEventTimes();
        this.marketModel          = marketModel;
    }

    // -------------------------------------------------------------------------
    // BehaviorRiskModelProvider interface
    // -------------------------------------------------------------------------

    @Override
    public Set<String> keys() {
        Set<String> keys = new HashSet<>();
        keys.add(this.riskFactorId);
        return keys;
    }

    @Override
    public List<CalloutData> contractStart(ContractModel contract) {
        // Contract-lifecycle scoped callouts: filter to [IED, maturityDate] window
        LocalDateTime ied = contract.getAs("initialExchangeDate");
        LocalDateTime maturity = contract.getAs("maturityDate");
        List<CalloutData> callouts = new ArrayList<>();
        for (String eventTime : this.monitoringEventTimes) {
            LocalDateTime eventDateTime = LocalDateTime.parse(eventTime);
            if (ied != null && eventDateTime.isBefore(ied)) {
                System.out.println("**** RedemptionPressureModel: SKIPPING pre-IED callout " + eventTime + " (IED=" + ied + ")");
                continue;
            }
            if (maturity != null && eventDateTime.isAfter(maturity)) {
                System.out.println("**** RedemptionPressureModel: SKIPPING post-maturity callout " + eventTime + " (maturity=" + maturity + ")");
                continue;
            }
            callouts.add(new CalloutData(this.riskFactorId, eventTime, CALLOUT_TYPE));
        }
        return callouts;
    }

    /**
     * Core redemption pressure logic.
     *
     * @param id     Risk factor ID
     * @param time   Current evaluation time
     * @param states Current contract state (notionalPrincipal = -outstanding_supply)
     * @return       Redemption fraction (0.0–1.0) applied as MRD to supply
     */
    @Override
    public double stateAt(String id, LocalDateTime time, StateSpace states) {

        double pegDeviation = this.marketModel.stateAt(this.pegDeviationMOC, time);
        double cashAvailable = this.marketModel.stateAt(this.cashReserveMOC, time);
        double outstandingSupply = Math.abs(states.notionalPrincipal);

        System.out.println("**** RedemptionPressureModel: time=" + time
                + " pegDeviation=" + String.format("%.4f", pegDeviation)
                + " cashAvailable=" + String.format("%.0f", cashAvailable)
                + " outstandingSupply=" + String.format("%.0f", outstandingSupply));

        // No pressure if peg deviation below threshold
        if (pegDeviation < this.pegDeviationThreshold) {
            return 0.0;
        }

        // Interpolate redemption rate from calibrated breakpoints
        double dailyRedemptionRate = interpolateRedemptionRate(pegDeviation);

        // Expected redemption volume
        double expectedRedemption = outstandingSupply * dailyRedemptionRate;

        // Check liquidity gap
        boolean liquidityGap = expectedRedemption > cashAvailable;

        System.out.println("**** RedemptionPressureModel: dailyRedemptionRate="
                + String.format("%.4f", dailyRedemptionRate)
                + " expectedRedemption=" + String.format("%.0f", expectedRedemption)
                + " liquidityGap=" + liquidityGap);

        if (liquidityGap) {
            // Can only honor redemptions up to cash available
            double honorableFraction = cashAvailable / outstandingSupply;
            System.out.println("**** RedemptionPressureModel: LIQUIDITY_GAP — "
                    + "can honor " + String.format("%.4f", honorableFraction)
                    + " of " + String.format("%.4f", dailyRedemptionRate) + " demanded");
            return Math.min(1.0, honorableFraction);
        }

        // Full redemption can be honored from cash
        return Math.min(1.0, dailyRedemptionRate);
    }

    // -------------------------------------------------------------------------
    // Piecewise linear interpolation across calibrated breakpoints
    // -------------------------------------------------------------------------

    private double interpolateRedemptionRate(double pegDeviation) {
        // Below first breakpoint
        if (pegDeviation <= DEG_PEG_BREAKPOINTS[0]) {
            return REDEMPTION_RATES[0] * (pegDeviation / DEG_PEG_BREAKPOINTS[0]);
        }
        // Above last breakpoint — cap at maximum
        if (pegDeviation >= DEG_PEG_BREAKPOINTS[DEG_PEG_BREAKPOINTS.length - 1]) {
            return REDEMPTION_RATES[REDEMPTION_RATES.length - 1];
        }
        // Interpolate between breakpoints
        for (int i = 0; i < DEG_PEG_BREAKPOINTS.length - 1; i++) {
            if (pegDeviation <= DEG_PEG_BREAKPOINTS[i + 1]) {
                double t = (pegDeviation - DEG_PEG_BREAKPOINTS[i])
                         / (DEG_PEG_BREAKPOINTS[i + 1] - DEG_PEG_BREAKPOINTS[i]);
                return REDEMPTION_RATES[i] + t * (REDEMPTION_RATES[i + 1] - REDEMPTION_RATES[i]);
            }
        }
        return REDEMPTION_RATES[REDEMPTION_RATES.length - 1];
    }
}
