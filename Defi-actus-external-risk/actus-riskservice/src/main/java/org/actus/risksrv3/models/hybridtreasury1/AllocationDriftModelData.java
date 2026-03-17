package org.actus.risksrv3.models.hybridtreasury1;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

/**
 * AllocationDriftModelData
 *
 * MongoDB document for AllocationDriftModel configuration.
 * Collection: allocationDriftModels
 *
 * Generic for any digital asset (BTC, ETH, etc.) — determined by
 * which spotPriceMOC is configured.
 *
 * JSON example for Postman POST /addAllocationDriftModel:
 * {
 *   "riskFactorId": "ad_btc01",
 *   "spotPriceMOC": "BTC_USD_SPOT",
 *   "portfolioTotalValueMOC": "PORTFOLIO_TOTAL_VALUE",
 *   "targetAllocation": 0.12,
 *   "maxAllocation": 0.15,
 *   "minAllocation": 0.09,
 *   "monitoringEventTimes": [
 *     "2026-03-01T00:00:00", "2026-03-02T00:00:00", ...
 *   ]
 * }
 *
 * For ETH:
 * {
 *   "riskFactorId": "ad_eth01",
 *   "spotPriceMOC": "ETH_USD_SPOT",
 *   "portfolioTotalValueMOC": "PORTFOLIO_TOTAL_VALUE",
 *   "targetAllocation": 0.08,
 *   "maxAllocation": 0.10,
 *   "minAllocation": 0.06,
 *   "monitoringEventTimes": [...]
 * }
 */
@Document(collection = "allocationDriftModels")
public class AllocationDriftModelData {

    @Id
    private String riskFactorId;

    private String spotPriceMOC;
    private String portfolioTotalValueMOC;
    private double targetAllocation;
    private double maxAllocation;
    private double minAllocation;
    private List<String> monitoringEventTimes;

    // ================================================================
    // CLM/UMP/PAM support: when BTC is modeled as a debt instrument
    // (e.g. CLM for yield-producing lending), notionalPrincipal is in
    // USD (e.g. $2,000,000), NOT in BTC units (e.g. 40). The model
    // needs to know the actual BTC quantity separately.
    //
    // positionQuantity: actual asset units (e.g. 40 BTC)
    //   - When > 0: assetValue = positionQuantity × spotPrice,
    //                scaled by (current notionalPrincipal / initialNotionalPrincipal)
    //                to reflect PP event reductions
    //   - When 0 or null: legacy STK/COM mode —
    //                assetValue = abs(notionalPrincipal) × spotPrice
    //                (notionalPrincipal IS the quantity)
    //
    // initialNotionalPrincipal: the starting notionalPrincipal from the
    //   contract JSON ($2,000,000 for CLM, 40 for STK). Used to compute
    //   the reduction factor after PP events.
    // ================================================================
    private double positionQuantity;            // e.g. 40.0 (BTC units)
    private double initialNotionalPrincipal;    // e.g. 2000000.0 (CLM) or 40.0 (STK)

    // ================================================================
    // Cash-mirror support: when an AllocationDriftModel is attached to
    // a cash/fiat contract to mirror BTC rebalancing in the opposite
    // direction. The same allocation signal is computed, then scaled
    // by signalMultiplier to convert to the correct fraction of the
    // cash contract's notionalPrincipal.
    //
    // signalMultiplier: scales the final allocation signal
    //   - Default 1.0 (normal behavior for BTC contract)
    //   - Set to -(btcPositionValue / cashNP) for cash mirror
    //     e.g. -(2000000 / 500000) = -4.0
    //   - Negative inverts direction: overweight BTC → sell BTC → buy cash
    //
    // useFixedQuantity: when true, positionQuantity is used as-is
    //   without scaling by NP reduction factor. Required for cash
    //   mirror because the cash contract's NP changes don't reflect
    //   BTC quantity changes.
    //   - Default false (normal CLM mode: scale by currentNP/initialNP)
    //   - Set true for cash mirror contracts
    // ================================================================
    private double signalMultiplier = 1.0;
    private boolean useFixedQuantity = false;

    // ================================================================
    // Mirror passthrough support: when set, this model does NOT compute
    // allocation independently. Instead, stateAt() reads the dollar
    // payoff cached by the source model and returns the equivalent
    // fraction of this contract's NP (inverted direction).
    //
    // This guarantees dollar-for-dollar matching: if the source model
    // sells $148K of BTC, the mirror adds exactly $148K to cash.
    //
    // When mirrorSourceModelId is set, spotPriceMOC, portfolioTotalValueMOC,
    // targetAllocation, positionQuantity etc. are ignored at runtime.
    // Only monitoringEventTimes is still used (for callout scheduling).
    //
    // REQUIREMENT: the source contract must appear BEFORE the mirror
    // contract in the simulation contracts array, so the source model's
    // stateAt() runs first and populates the dollar cache.
    // ================================================================
    private String mirrorSourceModelId;

    // ================================================================
    // FEATURE 1: Progressive Profit-Taking
    // Automatically locks in profits incrementally every +20% gain.
    // Hardcoded thresholds: +20%, +40%, +60%, +80%, +100%
    // Each threshold triggers selling 20% of remaining position.
    //
    // enableProgressiveProfit: feature flag (default false)
    // totalCostBasis: total USD invested (e.g. $2,000,000 for 20 BTC @ $100k)
    //   - Used to calculate profit percentage: (currentValue - costBasis) / costBasis
    //   - Updated after each profit-taking event (reduced proportionally)
    // ================================================================
    private boolean enableProgressiveProfit = false;
    private double totalCostBasis = 0.0;

    // ================================================================
    // FEATURE 2: CFO Discretion Scoring
    // Context-aware downside protection based on loss severity,
    // risk tolerance, and portfolio health.
    //
    // enableCFODiscretion: feature flag (default false)
    // riskTolerance: CONSERVATIVE (0.8), MODERATE (1.0), or AGGRESSIVE (1.2)
    //   - Controls how aggressively to react to losses
    //   - Lower = faster exits, Higher = more tolerant
    // portfolioHealthMOC: Reference Index for total portfolio value
    //   - e.g. "HT_PORTFOLIO_01" from JavaScript-derived data
    //   - Used to calculate portfolio health score (YTD performance)
    // cashBalanceMOC: Reference Index for cash balance
    //   - e.g. "HT_CASH_BAL_01" from JavaScript-derived data
    //   - Used to assess liquidity in portfolio health calculation
    //
    // Decision Score = (Loss Severity × Risk Weight) + Health Adjustment
    // Action based on score: 0-1 HOLD, 1-2 WATCH, 2-3 TRIM 25%,
    //                        3-4 TRIM 50%, 4-5 EXIT 75%, 5+ EXIT 100%
    // ================================================================
    private boolean enableCFODiscretion = false;
    private String riskTolerance = "MODERATE";
    private String portfolioHealthMOC = null;
    private String cashBalanceMOC = null;

    // ================================================================
    // FEATURE 3: Reload Queue
    // After stop-loss exit, preserves 50% of proceeds for re-entry.
    // Hardcoded recovery threshold: +30% from bottom price.
    //
    // enableReloadQueue: feature flag (default false)
    // reloadQueueUSD: amount in USD available for reload (runtime state)
    //   - Populated when 100% exit occurs (50% of proceeds)
    //   - Depleted when reload triggers
    // bottomPriceForReload: lowest price seen after exit (runtime state)
    //   - Tracked to determine +30% recovery trigger point
    //   - Reset when reload executes
    // ================================================================
    private boolean enableReloadQueue = false;
    private double reloadQueueUSD = 0.0;
    private double bottomPriceForReload = 0.0;

    // ================================================================
    // FEATURE 4: Minimum Position Retention (Floor Protection)
    // Prevents overselling by enforcing a minimum percentage of the
    // initial position that must be retained at all times.
    //
    // minPositionRetention: minimum fraction of initial position to keep
    //   - Range: 0.0 (no floor) to 1.0 (100% retention)
    //   - Example: 0.50 = retain at least 50% of starting position
    //   - Default: 0.0 (no floor protection - backward compatible)
    //   - Applied BEFORE all sell signals (PP, CFO, drift rebalancing)
    //   - Prevents the cumulative Progressive Profit sells from
    //     overselling the position beyond the initial quantity
    // ================================================================
    private double minPositionRetention = 0.0;

    // ================================================================
    // FEATURE 5: Configurable Progressive Profit Lock Percentage
    // Allows per-tier customization of how aggressively to lock profits
    // at each threshold, replacing the hardcoded 20% constant.
    //
    // progressiveProfitLockPercentage: fraction to sell per threshold
    //   - Range: 0.0 to 1.0
    //   - Example: 0.20 = sell 20% of current position at each threshold
    //   - Default: 0.20 (matches existing hardcoded behavior)
    //   - Lower values (e.g. 0.10) = let positions run longer
    //   - Higher values (e.g. 0.20) = lock profits more aggressively
    // ================================================================
    private double progressiveProfitLockPercentage = 0.20;

    public AllocationDriftModelData() {
    }

    // --- Getters and Setters ---

    public String getRiskFactorId() { return riskFactorId; }
    public void setRiskFactorId(String riskFactorId) { this.riskFactorId = riskFactorId; }

    public String getSpotPriceMOC() { return spotPriceMOC; }
    public void setSpotPriceMOC(String spotPriceMOC) { this.spotPriceMOC = spotPriceMOC; }

    public String getPortfolioTotalValueMOC() { return portfolioTotalValueMOC; }
    public void setPortfolioTotalValueMOC(String portfolioTotalValueMOC) { this.portfolioTotalValueMOC = portfolioTotalValueMOC; }

    public double getTargetAllocation() { return targetAllocation; }
    public void setTargetAllocation(double targetAllocation) { this.targetAllocation = targetAllocation; }

    public double getMaxAllocation() { return maxAllocation; }
    public void setMaxAllocation(double maxAllocation) { this.maxAllocation = maxAllocation; }

    public double getMinAllocation() { return minAllocation; }
    public void setMinAllocation(double minAllocation) { this.minAllocation = minAllocation; }

    public List<String> getMonitoringEventTimes() { return monitoringEventTimes; }
    public void setMonitoringEventTimes(List<String> monitoringEventTimes) { this.monitoringEventTimes = monitoringEventTimes; }

    public double getPositionQuantity() { return positionQuantity; }
    public void setPositionQuantity(double positionQuantity) { this.positionQuantity = positionQuantity; }

    public double getInitialNotionalPrincipal() { return initialNotionalPrincipal; }
    public void setInitialNotionalPrincipal(double initialNotionalPrincipal) { this.initialNotionalPrincipal = initialNotionalPrincipal; }

    public double getSignalMultiplier() { return signalMultiplier; }
    public void setSignalMultiplier(double signalMultiplier) { this.signalMultiplier = signalMultiplier; }

    public boolean isUseFixedQuantity() { return useFixedQuantity; }
    public void setUseFixedQuantity(boolean useFixedQuantity) { this.useFixedQuantity = useFixedQuantity; }

    public String getMirrorSourceModelId() { return mirrorSourceModelId; }
    public void setMirrorSourceModelId(String mirrorSourceModelId) { this.mirrorSourceModelId = mirrorSourceModelId; }

    // Progressive Profit-Taking
    public boolean isEnableProgressiveProfit() { return enableProgressiveProfit; }
    public void setEnableProgressiveProfit(boolean enableProgressiveProfit) { this.enableProgressiveProfit = enableProgressiveProfit; }

    public double getTotalCostBasis() { return totalCostBasis; }
    public void setTotalCostBasis(double totalCostBasis) { this.totalCostBasis = totalCostBasis; }

    // CFO Discretion
    public boolean isEnableCFODiscretion() { return enableCFODiscretion; }
    public void setEnableCFODiscretion(boolean enableCFODiscretion) { this.enableCFODiscretion = enableCFODiscretion; }

    public String getRiskTolerance() { return riskTolerance; }
    public void setRiskTolerance(String riskTolerance) { this.riskTolerance = riskTolerance; }

    public String getPortfolioHealthMOC() { return portfolioHealthMOC; }
    public void setPortfolioHealthMOC(String portfolioHealthMOC) { this.portfolioHealthMOC = portfolioHealthMOC; }

    public String getCashBalanceMOC() { return cashBalanceMOC; }
    public void setCashBalanceMOC(String cashBalanceMOC) { this.cashBalanceMOC = cashBalanceMOC; }

    // Reload Queue
    public boolean isEnableReloadQueue() { return enableReloadQueue; }
    public void setEnableReloadQueue(boolean enableReloadQueue) { this.enableReloadQueue = enableReloadQueue; }

    public double getReloadQueueUSD() { return reloadQueueUSD; }
    public void setReloadQueueUSD(double reloadQueueUSD) { this.reloadQueueUSD = reloadQueueUSD; }

    public double getBottomPriceForReload() { return bottomPriceForReload; }
    public void setBottomPriceForReload(double bottomPriceForReload) { this.bottomPriceForReload = bottomPriceForReload; }

    // Minimum Position Retention
    public double getMinPositionRetention() { return minPositionRetention; }
    public void setMinPositionRetention(double minPositionRetention) { 
        this.minPositionRetention = minPositionRetention; 
    }

    // Progressive Profit Lock Percentage
    public double getProgressiveProfitLockPercentage() { return progressiveProfitLockPercentage; }
    public void setProgressiveProfitLockPercentage(double progressiveProfitLockPercentage) { 
        this.progressiveProfitLockPercentage = progressiveProfitLockPercentage; 
    }
}
