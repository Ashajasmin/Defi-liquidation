package org.actus.risksrv3.utils;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.CollateralLTVModelData;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;

/**
 * CollateralLTVModel
 *
 * Monitors the Loan-to-Value ratio of an ETH-collateralised loan and returns
 * a repayment fraction (0.0-1.0) at each monitoring event time.
 *
 * Logic at each check date:
 *   collateralValue = collateralQuantity x currentETHPrice
 *   currentLTV      = notionalPrincipal / collateralValue
 *
 *   LTV >= liquidationThreshold  ->  return 1.0  (full forced liquidation)
 *   LTV >= ltvThreshold          ->  return fraction to restore LTV to ltvTarget
 *   LTV <  ltvThreshold          ->  return 0.0  (healthy, no action)
 *
 * Place this file in:
 *   src/main/java/org/actus/risksrv3/utils/
 */
public class CollateralLTVModel implements BehaviorRiskModelProvider {

    /** Callout event type - same as TwoDimensionalPrepaymentModel uses. */
    public static final String CALLOUT_TYPE = "MRD";

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final String riskFactorId;
    private final String collateralPriceMarketObjectCode;
    private final double collateralQuantity;
    private final double ltvThreshold;
    private final double ltvTarget;
    private final double liquidationThreshold;
    private final List<String> monitoringEventTimes;

    /** The active MultiMarketRiskModel - used to look up the ETH price. */
    private final MultiMarketRiskModel marketModel;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public CollateralLTVModel(String riskFactorId,
                              CollateralLTVModelData data,
                              MultiMarketRiskModel marketModel) {
        this.riskFactorId                     = riskFactorId;
        this.collateralPriceMarketObjectCode  = data.getCollateralPriceMarketObjectCode();
        this.collateralQuantity               = data.getCollateralQuantity();
        this.ltvThreshold                     = data.getLtvThreshold();
        this.ltvTarget                        = data.getLtvTarget();
        this.liquidationThreshold             = data.getLiquidationThreshold();
        this.monitoringEventTimes             = data.getMonitoringEventTimes();
        this.marketModel                      = marketModel;
    }

    // -------------------------------------------------------------------------
    // Methods - matching the pattern of TwoDimensionalPrepaymentModel
    // -------------------------------------------------------------------------

    public Set<String> keys() {
        Set<String> keys = new HashSet<>();
        keys.add(this.riskFactorId);
        return keys;
    }

    /**
     * Called at contract start - registers one MRD callout per monitoring date.
     */
    public List<CalloutData> contractStart(ContractModel contract) {
        // PP-before-IED fix: filter out callouts before contract starts
        LocalDateTime ied = contract.getAs("initialExchangeDate");
        List<CalloutData> callouts = new ArrayList<>();
        for (String eventTime : this.monitoringEventTimes) {
            if (ied != null) {
                LocalDateTime eventDateTime = LocalDateTime.parse(eventTime);
                if (eventDateTime.isBefore(ied)) {
                    System.out.println("**** CollateralLTVModel: SKIPPING pre-IED callout " + eventTime + " (IED=" + ied + ")");
                    continue;
                }
            }
            callouts.add(new CalloutData(this.riskFactorId, eventTime, CALLOUT_TYPE));
        }
        return callouts;
    }

    /**
     * Core LTV logic - called at each MRD event time by actus-core.
     *
     * @param id     Risk factor ID.
     * @param time   Current evaluation time.
     * @param states Current contract state (provides notionalPrincipal).
     * @return       Repayment fraction: 0.0 = no action, 1.0 = full liquidation.
     */
    public double stateAt(String id, LocalDateTime time, StateSpace states) {

        // Fetch current ETH price from market model
        double ethPrice = this.marketModel.stateAt(
                this.collateralPriceMarketObjectCode, time);

        // Guard against bad data
        if (ethPrice <= 0.0) {
            return 0.0;
        }

        double collateralValue = this.collateralQuantity * ethPrice;
        double currentLTV      = states.notionalPrincipal / collateralValue;

        System.out.println("**** CollateralLTVModel: time=" + time
                + " ethPrice=" + ethPrice
                + " collateralValue=" + collateralValue
                + " notionalPrincipal=" + states.notionalPrincipal
                + " LTV=" + currentLTV);

        if (currentLTV >= this.liquidationThreshold) {
            System.out.println("**** CollateralLTVModel: LIQUIDATION triggered LTV=" + currentLTV);
            return 1.0;

        } else if (currentLTV >= this.ltvThreshold) {
            double targetDebt    = this.ltvTarget * collateralValue;
            double repayFraction = (states.notionalPrincipal - targetDebt)
                                   / states.notionalPrincipal;
            System.out.println("**** CollateralLTVModel: PARTIAL REPAY fraction=" + repayFraction);
            return Math.max(0.0, repayFraction);

        } else {
            return 0.0;
        }
    }
}