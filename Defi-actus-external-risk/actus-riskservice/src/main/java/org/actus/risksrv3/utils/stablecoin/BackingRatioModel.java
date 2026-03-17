package org.actus.risksrv3.utils.stablecoin;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.stablecoin.BackingRatioModelData;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;
import org.actus.risksrv3.utils.MultiMarketRiskModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * BackingRatioModel (Domain 5 — Model 6.1)
 *
 * Monitors the backing ratio of a stablecoin reserve:
 *   backing_ratio = total_reserves / outstanding_supply
 *
 * Runs on the stablecoin liability contract (negative-notional PAM
 * representing outstanding token supply). At each daily monitoring point
 * via MRD callout, receives StateSpace containing notionalPrincipal
 * (current outstanding supply, negative). Looks up aggregate reserve
 * value from risksrv3 marketModel.
 *
 * Decision logic at each check date:
 *   backing_ratio >= 1.0                     → return 0.0  (fully backed)
 *   backing_ratio <  1.0                     → return (1 - backing_ratio) as unbacked fraction
 *   immediateLiquidity < liquidityThreshold  → return liquidityGap fraction
 *
 * Historical calibration (609 de-peg events):
 *   immediate liquidity > 35% → 94% recovery rate
 *   immediate liquidity < 20% → 23% recovery rate
 *
 * Regulatory references:
 *   STABLE Act:  backing >= 100%
 *   GENIUS Act:  backing >= 100%, HQLA composition, WAM <= 93 days
 *   MiCA EMT:   backing >= 100%
 *
 * Market Object Codes consumed:
 *   SC_TOTAL_RESERVES  — aggregate reserve value (cash + T-bills + other)
 *   SC_CASH_RESERVE    — immediately available cash component
 *
 * Place this file in:
 *   src/main/java/org/actus/risksrv3/utils/stablecoin/
 */
public class BackingRatioModel implements BehaviorRiskModelProvider {

    public static final String CALLOUT_TYPE = "MRD";

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final String riskFactorId;
    private final String totalReservesMOC;   // market object code for total reserves
    private final String cashReserveMOC;     // market object code for cash reserves
    private final double backingThreshold;   // regulatory minimum (1.0 = 100%)
    private final double liquidityThreshold; // minimum immediate liquidity ratio (0.35)
    private final List<String> monitoringEventTimes;
    private final MultiMarketRiskModel marketModel;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public BackingRatioModel(String riskFactorId,
                             BackingRatioModelData data,
                             MultiMarketRiskModel marketModel) {
        this.riskFactorId       = riskFactorId;
        this.totalReservesMOC   = data.getTotalReservesMOC();
        this.cashReserveMOC     = data.getCashReserveMOC();
        this.backingThreshold   = data.getBackingThreshold();
        this.liquidityThreshold = data.getLiquidityThreshold();
        this.monitoringEventTimes = data.getMonitoringEventTimes();
        this.marketModel        = marketModel;
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

    /**
     * Registers one MRD callout per monitoring date, scoped to
     * the contract lifecycle [IED, maturityDate] window.
     */
    @Override
    public List<CalloutData> contractStart(ContractModel contract) {
        // Contract-lifecycle scoped callouts: filter to [IED, maturityDate] window
        LocalDateTime ied = contract.getAs("initialExchangeDate");
        LocalDateTime maturity = contract.getAs("maturityDate");
        List<CalloutData> callouts = new ArrayList<>();
        for (String eventTime : this.monitoringEventTimes) {
            LocalDateTime eventDateTime = LocalDateTime.parse(eventTime);
            if (ied != null && eventDateTime.isBefore(ied)) {
                System.out.println("**** BackingRatioModel: SKIPPING pre-IED callout " + eventTime + " (IED=" + ied + ")");
                continue;
            }
            if (maturity != null && eventDateTime.isAfter(maturity)) {
                System.out.println("**** BackingRatioModel: SKIPPING post-maturity callout " + eventTime + " (maturity=" + maturity + ")");
                continue;
            }
            callouts.add(new CalloutData(this.riskFactorId, eventTime, CALLOUT_TYPE));
        }
        return callouts;
    }

    /**
     * Core backing ratio logic — called at each MRD event time.
     *
     * notionalPrincipal is NEGATIVE for a liability contract (outstanding supply).
     * We take Math.abs() to get positive supply figure.
     *
     * @param id     Risk factor ID
     * @param time   Current evaluation time
     * @param states Current contract state (notionalPrincipal = -outstanding_supply)
     * @return       0.0 if fully backed, else unbacked fraction (0.0–1.0)
     */
    @Override
    public double stateAt(String id, LocalDateTime time, StateSpace states) {

        // Fetch reserve values from market model
        double totalReserves = this.marketModel.stateAt(this.totalReservesMOC, time);
        double cashReserve   = this.marketModel.stateAt(this.cashReserveMOC, time);

        // Outstanding supply from contract state (negative notional = liability)
        double outstandingSupply = Math.abs(states.notionalPrincipal);

        // Guard against zero supply
        if (outstandingSupply <= 0.0) {
            System.out.println("**** BackingRatioModel: time=" + time
                    + " WARNING: outstandingSupply <= 0, returning 0.0");
            return 0.0;
        }

        // Compute ratios
        double backingRatio = totalReserves / outstandingSupply;
        double immediateLiquidity = cashReserve / outstandingSupply;

        System.out.println("**** BackingRatioModel: time=" + time
                + " totalReserves=" + totalReserves
                + " cashReserve=" + cashReserve
                + " outstandingSupply=" + outstandingSupply
                + " backingRatio=" + String.format("%.4f", backingRatio)
                + " immediateLiquidity=" + String.format("%.4f", immediateLiquidity));

        // Decision logic
        if (backingRatio >= this.backingThreshold) {
            // Fully backed — check liquidity stress
            if (immediateLiquidity < this.liquidityThreshold) {
                // Liquidity gap: reserves exist but are locked in non-liquid assets
                double liquidityGap = this.liquidityThreshold - immediateLiquidity;
                System.out.println("**** BackingRatioModel: LIQUIDITY_GAP=" 
                        + String.format("%.4f", liquidityGap)
                        + " (backed but illiquid)");
                return liquidityGap;
            }
            // Fully backed and adequately liquid
            return 0.0;
        } else {
            // Underbacked — return unbacked fraction
            double unbackedFraction = 1.0 - backingRatio;
            System.out.println("**** BackingRatioModel: UNDERBACKED fraction="
                    + String.format("%.4f", unbackedFraction));
            return Math.min(1.0, unbackedFraction);
        }
    }
}
