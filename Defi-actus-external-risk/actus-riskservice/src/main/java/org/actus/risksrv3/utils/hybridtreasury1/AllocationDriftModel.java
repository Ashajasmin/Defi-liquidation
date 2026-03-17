package org.actus.risksrv3.utils.hybridtreasury1;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.hybridtreasury1.AllocationDriftModelData;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;
import org.actus.risksrv3.utils.MultiMarketRiskModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AllocationDriftModel (Domain 4 — Treasury Model 5.1)
 *
 * Monitors the allocation of any digital asset (BTC, ETH, or other)
 * within a hybrid treasury portfolio. When the asset's market-value
 * share breaches maxAllocation, a rebalancing signal is returned.
 * When below minAllocation, an underweight signal is returned.
 *
 * Generic design: The same model class works for BTC, ETH, or any
 * digital asset — the specific asset is determined by which
 * spotPriceMOC is configured (e.g. BTC_USD_SPOT or ETH_USD_SPOT).
 * Multiple instances can run simultaneously (one per asset STK contract).
 *
 * Runs on a digital-asset STK contract. At each daily monitoring point
 * via MRD callout, receives StateSpace containing notionalPrincipal
 * (position value or units held). Looks up spot price and total
 * portfolio value from risksrv3 marketModel.
 *
 * Decision logic at each check date:
 *   asset_value = notionalPrincipal × spot_price
 *   allocation  = asset_value / portfolio_total_value
 *
 *   allocation > maxAllocation  → return (allocation - targetAllocation)
 *                                  positive signal = overweight, sell to rebalance
 *   allocation < minAllocation  → return -(targetAllocation - allocation)
 *                                  negative signal = underweight, buy to rebalance
 *   else                        → return 0.0  (within tolerance band)
 *
 * Portfolio context ($10M hybrid treasury):
 *   50% fiat ($5M cash + T-bills)
 *   30% stablecoin ($3M USDC)
 *   20% digital ($2M BTC/ETH)
 *   Per-asset targets configured independently
 *
 * ACTUS contract types supported:
 *   STK/COM  — notionalPrincipal IS the quantity (legacy mode)
 *   CLM/UMP/PAM — notionalPrincipal is in USD; positionQuantity provides
 *                 the actual asset units (e.g. 40 BTC). Reduction factor
 *                 from PP events is computed as:
 *                   currentNotional / initialNotionalPrincipal
 *
 * Market Object Codes consumed:
 *   [spotPriceMOC]           — e.g. BTC_USD_SPOT or ETH_USD_SPOT
 *   PORTFOLIO_TOTAL_VALUE    — total portfolio NAV
 */
public class AllocationDriftModel implements BehaviorRiskModelProvider {

    public static final String CALLOUT_TYPE = "MRD";

    // Hardcoded constants for progressive profit-taking
    private static final double[] PROFIT_THRESHOLDS = {0.20, 0.40, 0.60, 0.80, 1.00};

    // Hardcoded constant for reload queue
    private static final double RELOAD_RECOVERY_PCT = 0.30;

    private final String riskFactorId;
    private final String spotPriceMOC;
    private final String portfolioTotalValueMOC;
    private final double targetAllocation;
    private final double maxAllocation;
    private final double minAllocation;
    private final List<String> monitoringEventTimes;
    private final MultiMarketRiskModel marketModel;

    // CLM/UMP/PAM support fields
    private final double positionQuantity;           // 0 = legacy STK mode
    private final double initialNotionalPrincipal;   // 0 = legacy STK mode

    // Cash-mirror support fields
    private final double signalMultiplier;            // default 1.0; set -4.0 for cash mirror
    private final boolean useFixedQuantity;           // default false; true = bypass NP reduction scaling

    // Mirror passthrough support
    private final String mirrorSourceModelId;          // null = normal mode; set = read from source cache
    private AllocationDriftModel mirrorSource;          // wired by RiskObservationHandler after construction
    private final Map<LocalDateTime, Double> dollarPayoffCache = new HashMap<>();  // time → dollar amount from stateAt()

    // Feature 1: Progressive Profit-Taking
    private final boolean enableProgressiveProfit;
    private double totalCostBasis;
    private final boolean[] profitLockedFlags = new boolean[5];  // runtime state for 5 thresholds

    // Feature 2: CFO Discretion
    private final boolean enableCFODiscretion;
    private final String riskTolerance;
    private final String portfolioHealthMOC;
    private final String cashBalanceMOC;
    private double initialPortfolioValue = 0.0;  // runtime state for YTD calculation

    // Feature 3: Reload Queue
    private final boolean enableReloadQueue;
    private double reloadQueueUSD;
    private double bottomPriceForReload;

    // Feature 4: Minimum Position Retention
    private final double minPositionRetention;

    // Feature 5: Progressive Profit Lock Percentage
    private final double progressiveProfitLockPercentage;

    public AllocationDriftModel(String riskFactorId,
                                AllocationDriftModelData data,
                                MultiMarketRiskModel marketModel) {
        this.riskFactorId              = riskFactorId;
        this.spotPriceMOC              = data.getSpotPriceMOC();
        this.portfolioTotalValueMOC    = data.getPortfolioTotalValueMOC();
        this.targetAllocation          = data.getTargetAllocation();
        this.maxAllocation             = data.getMaxAllocation();
        this.minAllocation             = data.getMinAllocation();
        this.monitoringEventTimes      = data.getMonitoringEventTimes();
        this.marketModel               = marketModel;
        this.positionQuantity          = data.getPositionQuantity();
        this.initialNotionalPrincipal  = data.getInitialNotionalPrincipal();
        this.signalMultiplier          = data.getSignalMultiplier();
        this.useFixedQuantity          = data.isUseFixedQuantity();
        this.mirrorSourceModelId       = data.getMirrorSourceModelId();

        // Feature 1: Progressive Profit-Taking
        this.enableProgressiveProfit   = data.isEnableProgressiveProfit();
        this.totalCostBasis            = data.getTotalCostBasis();

        // Feature 2: CFO Discretion
        this.enableCFODiscretion       = data.isEnableCFODiscretion();
        this.riskTolerance             = data.getRiskTolerance();
        this.portfolioHealthMOC        = data.getPortfolioHealthMOC();
        this.cashBalanceMOC            = data.getCashBalanceMOC();

        // Feature 3: Reload Queue
        this.enableReloadQueue         = data.isEnableReloadQueue();
        this.reloadQueueUSD            = data.getReloadQueueUSD();
        this.bottomPriceForReload      = data.getBottomPriceForReload();

        // Feature 4: Minimum Position Retention
        this.minPositionRetention      = data.getMinPositionRetention();

        // Feature 5: Progressive Profit Lock Percentage
        this.progressiveProfitLockPercentage = data.getProgressiveProfitLockPercentage();
    }

    /** Called by RiskObservationHandler to wire the source model reference */
    public void setMirrorSource(AllocationDriftModel source) {
        this.mirrorSource = source;
    }

    /** Returns the mirrorSourceModelId from config (null if normal mode) */
    public String getMirrorSourceModelId() {
        return this.mirrorSourceModelId;
    }

    /** Read cached dollar payoff for a given time (used by mirror models) */
    public Double getCachedDollarPayoff(LocalDateTime time) {
        return this.dollarPayoffCache.get(time);
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
            if (ied != null) {
                LocalDateTime eventDateTime = LocalDateTime.parse(eventTime);
                if (eventDateTime.isBefore(ied)) {
                    System.out.println("**** AllocationDriftModel: SKIPPING pre-IED callout " + eventTime + " (IED=" + ied + ")");
                    continue;
                }
            }
            callouts.add(new CalloutData(this.riskFactorId, eventTime, CALLOUT_TYPE));
        }
        return callouts;
    }

    /**
     * FEATURE 1 HELPER: Check if progressive profit-taking threshold is met
     * Returns the percentage to sell (0.0-1.0) if a threshold is hit, 0.0 otherwise
     */
    private double checkProgressiveProfit(double currentValue, double quantity) {
        if (!this.enableProgressiveProfit || this.totalCostBasis <= 0.0 || quantity <= 0.0) {
            return 0.0;
        }

        double profitPct = (currentValue - this.totalCostBasis) / this.totalCostBasis;

        for (int i = 0; i < PROFIT_THRESHOLDS.length; i++) {
            if (profitPct >= PROFIT_THRESHOLDS[i] && !this.profitLockedFlags[i]) {
                // Lock this threshold
                this.profitLockedFlags[i] = true;

                // Sell progressiveProfitLockPercentage of current position
                double sellFraction = this.progressiveProfitLockPercentage;

                // Update cost basis: reduce proportionally by the sell fraction
                this.totalCostBasis = this.totalCostBasis * (1.0 - sellFraction);

                System.out.println("**** AllocationDriftModel [" + this.riskFactorId + "]: PROGRESSIVE PROFIT"
                        + " threshold=" + String.format("%.0f%%", PROFIT_THRESHOLDS[i] * 100)
                        + " profitPct=" + String.format("%.2f%%", profitPct * 100)
                        + " → SELL " + String.format("%.0f%%", sellFraction * 100)
                        + " newCostBasis=" + String.format("%.2f", this.totalCostBasis));

                return sellFraction;
            }
        }

        return 0.0;
    }

    /**
     * FEATURE 2 HELPER: Calculate CFO discretion score based on loss severity,
     * risk tolerance, and portfolio health
     * Returns the percentage to sell (0.0-1.0) based on the discretion score
     */
    private double checkCFODiscretion(double currentValue, LocalDateTime time) {
        if (!this.enableCFODiscretion || this.totalCostBasis <= 0.0) {
            return 0.0;
        }

        // Calculate loss percentage
        double lossPct = (currentValue - this.totalCostBasis) / this.totalCostBasis;

        // Only trigger on losses
        if (lossPct >= 0.0) {
            return 0.0;
        }

        // Determine loss severity (1-5)
        int severity;
        double absLoss = Math.abs(lossPct);
        if (absLoss >= 0.30) {
            severity = 5;
        } else if (absLoss >= 0.20) {
            severity = 4;
        } else if (absLoss >= 0.15) {
            severity = 3;
        } else if (absLoss >= 0.10) {
            severity = 2;
        } else if (absLoss >= 0.05) {
            severity = 1;
        } else {
            return 0.0;  // Loss too small
        }

        // Map risk tolerance to weight
        double riskWeight;
        if ("CONSERVATIVE".equals(this.riskTolerance)) {
            riskWeight = 0.8;
        } else if ("AGGRESSIVE".equals(this.riskTolerance)) {
            riskWeight = 1.2;
        } else {  // MODERATE or any other value
            riskWeight = 1.0;
        }

        // Calculate portfolio health adjustment
        double healthAdjustment = calculatePortfolioHealthAdjustment(time);

        // Calculate discretion score
        double discretionScore = (severity * riskWeight) + healthAdjustment;

        System.out.println("**** AllocationDriftModel [" + this.riskFactorId + "]: CFO DISCRETION"
                + " loss=" + String.format("%.2f%%", lossPct * 100)
                + " severity=" + severity
                + " riskWeight=" + String.format("%.1f", riskWeight)
                + " healthAdj=" + String.format("%.1f", healthAdjustment)
                + " score=" + String.format("%.2f", discretionScore));

        // Map score to action
        if (discretionScore >= 5.0) {
            return 1.00;  // EXIT 100%
        } else if (discretionScore >= 4.0) {
            return 0.75;  // EXIT 75%
        } else if (discretionScore >= 3.0) {
            return 0.50;  // TRIM 50%
        } else if (discretionScore >= 2.0) {
            return 0.25;  // TRIM 25%
        } else {
            return 0.0;   // HOLD or WATCH
        }
    }

    /**
     * FEATURE 2 HELPER: Calculate portfolio health adjustment
     * Returns adjustment value between -1.0 and +2.0
     */
    private double calculatePortfolioHealthAdjustment(LocalDateTime time) {
        if (this.portfolioHealthMOC == null || this.cashBalanceMOC == null) {
            return 0.0;  // No health data available
        }

        try {
            double currentPortfolio = this.marketModel.stateAt(this.portfolioHealthMOC, time);
            double cashBalance = this.marketModel.stateAt(this.cashBalanceMOC, time);

            // Initialize first portfolio value for YTD calculation
            if (this.initialPortfolioValue <= 0.0) {
                this.initialPortfolioValue = currentPortfolio;
            }

            // Calculate YTD performance
            double ytdPerformance = (currentPortfolio - this.initialPortfolioValue) / this.initialPortfolioValue;

            // Calculate cash percentage
            double cashPct = cashBalance / currentPortfolio;

            // Determine health score (0-5)
            int healthScore;
            if (ytdPerformance >= 0.20 && cashPct >= 0.15) {
                healthScore = 5;  // Excellent
            } else if (ytdPerformance >= 0.10 && cashPct >= 0.12) {
                healthScore = 4;  // Good
            } else if (ytdPerformance >= 0.0 && cashPct >= 0.10) {
                healthScore = 3;  // Adequate
            } else if (ytdPerformance >= -0.05 && cashPct >= 0.08) {
                healthScore = 2;  // Concerning
            } else if (ytdPerformance >= -0.10) {
                healthScore = 1;  // Poor
            } else {
                healthScore = 0;  // Critical
            }

            // Map health score to adjustment
            if (healthScore == 5) {
                return -1.0;  // Excellent: reduce urgency
            } else if (healthScore == 4) {
                return -0.5;  // Good: slightly reduce urgency
            } else if (healthScore == 3) {
                return 0.0;   // Adequate: no adjustment
            } else if (healthScore == 2) {
                return 0.5;   // Concerning: increase urgency
            } else if (healthScore == 1) {
                return 1.0;   // Poor: significantly increase urgency
            } else {
                return 2.0;   // Critical: maximum urgency
            }

        } catch (Exception e) {
            System.out.println("**** AllocationDriftModel [" + this.riskFactorId + "]: WARNING"
                    + " failed to calculate portfolio health: " + e.getMessage());
            return 0.0;  // Default to no adjustment on error
        }
    }

    /**
     * FEATURE 3 HELPER: Check if reload opportunity exists (price recovered +30% from bottom)
     * Returns the quantity to buy if reload should trigger, 0.0 otherwise
     */
    private double checkReloadOpportunity(double currentPrice, double quantity) {
        if (!this.enableReloadQueue || this.reloadQueueUSD <= 0.0 || quantity > 0.0) {
            return 0.0;  // No reload capital or already holding position
        }

        // Track bottom price after exit
        if (this.bottomPriceForReload <= 0.0) {
            this.bottomPriceForReload = currentPrice;
            return 0.0;
        }

        // Update bottom if price drops further
        if (currentPrice < this.bottomPriceForReload) {
            this.bottomPriceForReload = currentPrice;
            return 0.0;
        }

        // Check for +30% recovery from bottom
        double recoveryPct = (currentPrice - this.bottomPriceForReload) / this.bottomPriceForReload;
        if (recoveryPct >= RELOAD_RECOVERY_PCT) {
            // Calculate how much to buy with reload capital
            double buyQuantity = this.reloadQueueUSD / currentPrice;

            System.out.println("**** AllocationDriftModel [" + this.riskFactorId + "]: RELOAD OPPORTUNITY"
                    + " bottomPrice=" + String.format("%.2f", this.bottomPriceForReload)
                    + " currentPrice=" + String.format("%.2f", currentPrice)
                    + " recovery=" + String.format("%.2f%%", recoveryPct * 100)
                    + " deployUSD=" + String.format("%.2f", this.reloadQueueUSD)
                    + " → BUY " + String.format("%.6f", buyQuantity) + " units");

            // Reset reload state
            this.reloadQueueUSD = 0.0;
            this.bottomPriceForReload = 0.0;

            // Return negative fraction to signal BUY
            // The quantity represents how much of the position to add
            return -buyQuantity;  // Negative = buy signal
        }

        return 0.0;
    }

    /**
     * FEATURE 3 HELPER: Handle 100% exit by setting up reload queue
     */
    private void handleFullExit(double exitProceeds) {
        if (!this.enableReloadQueue || exitProceeds <= 0.0) {
            return;
        }

        // Split proceeds: 50% to reload queue, 50% stays as T-Bills (handled by contract)
        this.reloadQueueUSD = exitProceeds * 0.50;

        System.out.println("**** AllocationDriftModel [" + this.riskFactorId + "]: RELOAD QUEUE SET"
                + " exitProceeds=" + String.format("%.2f", exitProceeds)
                + " reloadCapital=" + String.format("%.2f", this.reloadQueueUSD));
    }

    /**
     * FEATURE 4 HELPER: Apply position floor protection
     * Prevents selling below the minimum retention threshold
     * 
     * @param sellSignal The proposed sell signal (0.0-1.0)
     * @param quantity Current position quantity
     * @return Adjusted sell signal that respects the floor, or 0.0 if floor would be violated
     */
    private double applyPositionFloor(double sellSignal, double quantity) {
        if (this.minPositionRetention <= 0.0 || sellSignal <= 0.0 || quantity <= 0.0) {
            return sellSignal;  // No floor protection, no sell, or no position
        }

        // Calculate minimum quantity that must be retained (anchored to original positionQuantity)
        double minQuantity = this.positionQuantity * this.minPositionRetention;

        // Calculate maximum sellable quantity without breaching the floor
        double maxSellableQuantity = Math.max(0.0, quantity - minQuantity);

        // Convert to a signal fraction of current quantity
        double maxSellSignal = maxSellableQuantity / quantity;

        // CLIP (not block): reduce signal to exactly reach the floor, never go below it
        if (sellSignal > maxSellSignal) {
            double clippedSignal = Math.max(0.0, maxSellSignal);
            System.out.println("**** AllocationDriftModel [" + this.riskFactorId + "]: FLOOR CLIP"
                    + " proposedSell=" + String.format("%.2f%%", sellSignal * 100)
                    + " clippedTo=" + String.format("%.2f%%", clippedSignal * 100)
                    + " quantity=" + String.format("%.6f", quantity)
                    + " floor=" + String.format("%.6f", minQuantity)
                    + " maxSellable=" + String.format("%.6f", maxSellableQuantity)
                    + " (" + String.format("%.0f", this.minPositionRetention * 100) + "% of initial "
                    + String.format("%.6f", this.positionQuantity) + ")");
            return clippedSignal;  // Partial sell that exactly reaches the floor
        }

        return sellSignal;  // Sell is within limits, allow it unchanged
    }

    @Override
    public double stateAt(String id, LocalDateTime time, StateSpace states) {

        // ================================================================
        // MIRROR PASSTHROUGH MODE
        // If this model is a mirror, read the source model's cached dollar
        // payoff and return the equivalent fraction of THIS contract's NP
        // (inverted direction: source sells → mirror receives).
        // ================================================================
        if (this.mirrorSourceModelId != null && this.mirrorSource != null) {
            Double sourceDollars = this.mirrorSource.getCachedDollarPayoff(time);
            double currentNP = Math.abs(states.notionalPrincipal);

            if (sourceDollars == null || sourceDollars == 0.0 || currentNP <= 0.0) {
                System.out.println("**** AllocationDriftModel [" + this.riskFactorId + "]: MIRROR time=" + time
                        + " sourceDollars=" + sourceDollars + " currentNP=" + String.format("%.2f", currentNP)
                        + " → signal=0.0 (no source payoff)");
                return 0.0;
            }

            // sourceDollars is positive when source SOLD (PP payoff > 0)
            // Mirror should RECEIVE that amount: signal = -(dollars / NP)
            // STF_PP_rf2 applies: NP -= signal * NP = NP -= (-(dollars/NP)) * NP = NP += dollars
            double mirrorSignal = -(sourceDollars / currentNP);

            System.out.println("**** AllocationDriftModel [" + this.riskFactorId + "]: MIRROR time=" + time
                    + " sourceDollars=" + String.format("%.2f", sourceDollars)
                    + " currentNP=" + String.format("%.2f", currentNP)
                    + " → mirrorSignal=" + String.format("%.6f", mirrorSignal)
                    + " (cash will change by " + String.format("%.2f", sourceDollars) + ")");
            return mirrorSignal;
        }

        double spotPrice = this.marketModel.stateAt(this.spotPriceMOC, time);
        double portfolioTotal = this.marketModel.stateAt(this.portfolioTotalValueMOC, time);

        // ================================================================
        // Compute effective quantity of the digital asset
        //
        // Three modes of operation:
        //
        // 1. FIXED mode (useFixedQuantity == true):
        //    positionQuantity is used as-is, no NP reduction scaling.
        //    Used for cash-mirror contracts where this contract's NP
        //    is the cash balance, not the BTC position.
        //    Example: positionQuantity=40 (BTC), cash NP=$500K
        //
        // 2. CLM/UMP/PAM mode (positionQuantity > 0, useFixedQuantity == false):
        //    notionalPrincipal is in USD (e.g. $2,000,000)
        //    positionQuantity is the actual BTC units (e.g. 40)
        //    PP events reduce notionalPrincipal proportionally
        //    Effective quantity = positionQuantity × (currentNP / initialNP)
        //    Example: after 20% PP, NP = $1.6M, qty = 40 × (1.6M/2M) = 32
        //
        // 3. Legacy STK/COM mode (positionQuantity == 0):
        //    notionalPrincipal IS the quantity (e.g. 40 BTC)
        //    PP events reduce it directly (40 → 32 after 20% sell)
        // ================================================================
        double quantity;
        String quantityMode;
        if (this.useFixedQuantity && this.positionQuantity > 0) {
            // FIXED mode: use positionQuantity as-is (cash mirror)
            quantity = this.positionQuantity;
            quantityMode = "FIXED";
        } else if (this.positionQuantity > 0 && this.initialNotionalPrincipal > 0) {
            // CLM/UMP/PAM mode: scale positionQuantity by PP reduction factor
            double currentNP = Math.abs(states.notionalPrincipal);
            double reductionFactor = currentNP / this.initialNotionalPrincipal;
            quantity = this.positionQuantity * reductionFactor;
            quantityMode = "CLM";
        } else {
            // Legacy STK/COM mode: notionalPrincipal IS the quantity
            quantity = Math.abs(states.notionalPrincipal);
            quantityMode = "STK";
        }

        if (portfolioTotal <= 0.0) {
            System.out.println("**** AllocationDriftModel [" + this.riskFactorId + "]: time=" + time
                    + " WARNING: portfolioTotal <= 0, returning 0.0");
            return 0.0;
        }

        double assetValue = quantity * spotPrice;
        double allocation = assetValue / portfolioTotal;

        System.out.println("**** AllocationDriftModel [" + this.riskFactorId + "]: time=" + time
                + " mode=" + quantityMode
                + " spotPriceMOC=" + this.spotPriceMOC
                + " spotPrice=" + String.format("%.2f", spotPrice)
                + " quantity=" + String.format("%.6f", quantity)
                + " assetValue=" + String.format("%.2f", assetValue)
                + " portfolioTotal=" + String.format("%.2f", portfolioTotal)
                + " allocation=" + String.format("%.4f", allocation)
                + " target=" + targetAllocation
                + " max=" + maxAllocation
                + " min=" + minAllocation
                + " signalMultiplier=" + signalMultiplier);

        // ================================================================
        // ENHANCED LOGIC: Check features in priority order
        // ================================================================

        // PRIORITY 1: Progressive Profit-Taking
        double profitSignal = checkProgressiveProfit(assetValue, quantity);
        if (profitSignal > 0.0) {
            // Apply floor protection before finalizing the signal
            profitSignal = applyPositionFloor(profitSignal, quantity);
            if (profitSignal == 0.0) {
                this.dollarPayoffCache.put(time, 0.0);
                return 0.0;  // Floor blocked the sell
            }
            double finalSignal = profitSignal * this.signalMultiplier;
            double dollarPayoff = finalSignal * Math.abs(states.notionalPrincipal);
            this.dollarPayoffCache.put(time, dollarPayoff);
            System.out.println("**** AllocationDriftModel [" + this.riskFactorId + "]: PROGRESSIVE PROFIT TRIGGERED"
                    + " → finalSignal=" + String.format("%.6f", finalSignal)
                    + " dollarPayoff=" + String.format("%.2f", dollarPayoff) + " (cached)");
            return finalSignal;
        }

        // PRIORITY 2: CFO Discretion
        double discretionSignal = checkCFODiscretion(assetValue, time);
        if (discretionSignal > 0.0) {
            // Apply floor protection before finalizing the signal
            discretionSignal = applyPositionFloor(discretionSignal, quantity);
            if (discretionSignal == 0.0) {
                this.dollarPayoffCache.put(time, 0.0);
                return 0.0;  // Floor blocked the sell
            }
            double finalSignal = discretionSignal * this.signalMultiplier;
            double dollarPayoff = finalSignal * Math.abs(states.notionalPrincipal);
            this.dollarPayoffCache.put(time, dollarPayoff);

            // If 100% exit, set up reload queue
            if (discretionSignal >= 1.0) {
                handleFullExit(dollarPayoff);
            }

            System.out.println("**** AllocationDriftModel [" + this.riskFactorId + "]: CFO DISCRETION TRIGGERED"
                    + " → finalSignal=" + String.format("%.6f", finalSignal)
                    + " dollarPayoff=" + String.format("%.2f", dollarPayoff) + " (cached)");
            return finalSignal;
        }

        // PRIORITY 3: Reload Queue
        double reloadSignal = checkReloadOpportunity(spotPrice, quantity);
        if (reloadSignal < 0.0) {
            // Reload returns negative quantity to buy
            // Convert to allocation signal based on portfolio
            double buyValue = Math.abs(reloadSignal) * spotPrice;
            double buyAllocation = buyValue / portfolioTotal;
            double finalSignal = -buyAllocation * this.signalMultiplier;
            double dollarPayoff = finalSignal * Math.abs(states.notionalPrincipal);
            this.dollarPayoffCache.put(time, dollarPayoff);
            System.out.println("**** AllocationDriftModel [" + this.riskFactorId + "]: RELOAD TRIGGERED"
                    + " → finalSignal=" + String.format("%.6f", finalSignal)
                    + " dollarPayoff=" + String.format("%.2f", dollarPayoff) + " (cached)");
            return finalSignal;
        }

        // PRIORITY 4: Normal Drift Rebalancing (existing logic)
        if (allocation > this.maxAllocation) {
            double driftFraction = allocation - this.targetAllocation;
            driftFraction = Math.min(1.0, driftFraction);
            // Apply floor protection before finalizing the signal
            driftFraction = applyPositionFloor(driftFraction, quantity);
            if (driftFraction == 0.0) {
                this.dollarPayoffCache.put(time, 0.0);
                System.out.println("**** AllocationDriftModel [" + this.riskFactorId + "]: OVERWEIGHT but FLOOR blocks sell");
                return 0.0;
            }
            double finalSignal = driftFraction * this.signalMultiplier;
            double dollarPayoff = finalSignal * Math.abs(states.notionalPrincipal);
            this.dollarPayoffCache.put(time, dollarPayoff);
            System.out.println("**** AllocationDriftModel [" + this.riskFactorId + "]: OVERWEIGHT drift="
                    + String.format("%.4f", driftFraction)
                    + " × multiplier=" + String.format("%.2f", signalMultiplier)
                    + " → finalSignal=" + String.format("%.6f", finalSignal)
                    + " dollarPayoff=" + String.format("%.2f", dollarPayoff) + " (cached)");
            return finalSignal;
        } else if (allocation < this.minAllocation) {
            // NOTE: Floor does NOT apply to BUY signals (negative), only SELL signals
            double driftFraction = this.targetAllocation - allocation;
            double finalSignal = -Math.min(1.0, driftFraction) * this.signalMultiplier;
            double dollarPayoff = finalSignal * Math.abs(states.notionalPrincipal);
            this.dollarPayoffCache.put(time, dollarPayoff);
            System.out.println("**** AllocationDriftModel [" + this.riskFactorId + "]: UNDERWEIGHT drift="
                    + String.format("%.4f", driftFraction)
                    + " × multiplier=" + String.format("%.2f", signalMultiplier)
                    + " → finalSignal=" + String.format("%.6f", finalSignal)
                    + " dollarPayoff=" + String.format("%.2f", dollarPayoff) + " (cached)");
            return finalSignal;
        }

        // PRIORITY 5: Hold (no action)
        this.dollarPayoffCache.put(time, 0.0);
        System.out.println("**** AllocationDriftModel [" + this.riskFactorId + "]: WITHIN BAND → signal=0.0 (cached)");
        return 0.0;
    }
}
