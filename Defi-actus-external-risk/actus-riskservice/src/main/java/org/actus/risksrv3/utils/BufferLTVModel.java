package org.actus.risksrv3.utils;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.BufferLTVModelData;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * BufferLTVModel
 *
 * Implements a sophisticated buffer-first LTV defense strategy for DeFi lending.
 * 
 * STRATEGY:
 * - Monitors LTV based on ORIGINAL collateral quantity (constant 3.0 ETH)
 * - Funds prepayments from BUFFER reserve (depleting 4.0 ETH)
 * - Preserves collateral quantity throughout
 * - Implements circuit breaker to prevent "falling knife" losses
 *
 * CIRCUIT BREAKER CONDITIONS:
 * 1. Maximum interventions reached (e.g., 3 times)
 * 2. Buffer depleted below minimum reserve (e.g., 1.0 ETH)
 * 3. Falling knife detected (e.g., 20% price drop in 48 hours)
 * 4. Cooldown period active (e.g., 24 hours since last intervention)
 *
 * This model follows the BehaviorRiskModelProvider interface pattern
 * established by CollateralLTVModel and TwoDimensionalPrepaymentModel.
 */
public class BufferLTVModel implements BehaviorRiskModelProvider {

    /** Callout event type - same as CollateralLTVModel */
    public static final String CALLOUT_TYPE = "MRD";

    // -------------------------------------------------------------------------
    // Configuration Fields (immutable after construction)
    // -------------------------------------------------------------------------

    private final String riskFactorId;
    private final String collateralPriceMarketObjectCode;
    private final double collateralQuantity;
    private final String bufferContractId;
    private final double initialBufferQuantity;
    private final double ltvThreshold;
    private final double ltvTarget;
    private final double liquidationThreshold;
    private final int maxInterventions;
    private final double minBufferReserve;
    private final double maxBufferUsagePerIntervention;
    private final long cooldownMillis;
    private final double fallingKnifePriceDropThreshold;
    private final long fallingKnifeTimeWindowMillis;
    private final List<String> monitoringEventTimes;
    private final MultiMarketRiskModel marketModel;

    // -------------------------------------------------------------------------
    // State Fields (mutable, track intervention history)
    // -------------------------------------------------------------------------

    private int interventionCount = 0;
    private double currentBufferQuantity;
    private LocalDateTime lastInterventionTime = null;
    private LocalDateTime firstInterventionTime = null;
    private double priceAtFirstIntervention = 0.0;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public BufferLTVModel(String riskFactorId,
                          BufferLTVModelData data,
                          MultiMarketRiskModel marketModel) {
        this.riskFactorId = riskFactorId;
        this.collateralPriceMarketObjectCode = data.getCollateralPriceMarketObjectCode();
        this.collateralQuantity = data.getCollateralQuantity();
        this.bufferContractId = data.getBufferContractId();
        this.initialBufferQuantity = data.getInitialBufferQuantity();
        this.ltvThreshold = data.getLtvThreshold();
        this.ltvTarget = data.getLtvTarget();
        this.liquidationThreshold = data.getLiquidationThreshold();
        this.maxInterventions = data.getMaxInterventions();
        this.minBufferReserve = data.getMinBufferReserve();
        this.maxBufferUsagePerIntervention = data.getMaxBufferUsagePerIntervention();
        this.cooldownMillis = data.getCooldownMillis();
        this.fallingKnifePriceDropThreshold = data.getFallingKnifePriceDropThreshold();
        this.fallingKnifeTimeWindowMillis = data.getFallingKnifeTimeWindowMillis();
        this.monitoringEventTimes = data.getMonitoringEventTimes();
        this.marketModel = marketModel;

        // Initialize buffer quantity
        this.currentBufferQuantity = this.initialBufferQuantity;

        System.out.println("**** BufferLTVModel initialized: " + riskFactorId);
        System.out.println("  Collateral: " + collateralQuantity + " ETH (constant)");
        System.out.println("  Buffer: " + initialBufferQuantity + " ETH (initial)");
        System.out.println("  Max interventions: " + maxInterventions);
        System.out.println("  Min reserve: " + minBufferReserve + " ETH");
    }

    // -------------------------------------------------------------------------
    // BehaviorRiskModelProvider Interface Methods
    // -------------------------------------------------------------------------

    @Override
    public Set<String> keys() {
        Set<String> keys = new HashSet<>();
        keys.add(this.riskFactorId);
        return keys;
    }

    /**
     * Called at contract start - registers one MRD callout per monitoring date.
     */
    @Override
    public List<CalloutData> contractStart(ContractModel contract) {
        LocalDateTime ied = contract.getAs("initialExchangeDate");
        List<CalloutData> callouts = new ArrayList<>();
        
        for (String eventTime : this.monitoringEventTimes) {
            if (ied != null) {
                LocalDateTime eventDateTime = LocalDateTime.parse(eventTime);
                if (eventDateTime.isBefore(ied)) {
                    System.out.println("**** BufferLTVModel: SKIPPING pre-IED callout " 
                        + eventTime + " (IED=" + ied + ")");
                    continue;
                }
            }
            callouts.add(new CalloutData(this.riskFactorId, eventTime, CALLOUT_TYPE));
        }
        
        return callouts;
    }

    /**
     * Core buffer-first LTV logic - called at each MRD event time.
     *
     * DECISION FLOW:
     * 1. Calculate current LTV (using ORIGINAL collateral quantity)
     * 2. Check if intervention needed (LTV >= threshold)
     * 3. Evaluate circuit breaker conditions
     * 4. Check cooldown period
     * 5. Calculate buffer utilization
     * 6. Execute intervention (sell from buffer, not collateral)
     *
     * @param id     Risk factor ID
     * @param time   Current evaluation time
     * @param states Current contract state (provides notionalPrincipal)
     * @return       Repayment fraction: 0.0 = no action, 0.0-1.0 = partial repayment
     */
    @Override
    public double stateAt(String id, LocalDateTime time, StateSpace states) {

        // Step 1: Fetch current ETH price
        double ethPrice = this.marketModel.stateAt(
            this.collateralPriceMarketObjectCode, time);

        if (ethPrice <= 0.0) {
            System.out.println("**** BufferLTVModel WARNING: Invalid ETH price at " + time);
            return 0.0;
        }

        // Step 2: Calculate LTV using ORIGINAL collateral quantity (constant)
        double collateralValue = this.collateralQuantity * ethPrice;
        double currentLTV = states.notionalPrincipal / collateralValue;

        System.out.println("**** BufferLTVModel: time=" + time
            + " ethPrice=" + String.format("%.2f", ethPrice)
            + " collateralValue=" + String.format("%.2f", collateralValue)
            + " notionalPrincipal=" + String.format("%.2f", states.notionalPrincipal)
            + " LTV=" + String.format("%.2f%%", currentLTV * 100)
            + " buffer=" + String.format("%.4f", currentBufferQuantity) + " ETH");

        // Step 3: Check if liquidation threshold breached (emergency)
        if (currentLTV >= this.liquidationThreshold) {
            System.out.println("**** BufferLTVModel: LIQUIDATION THRESHOLD breached! LTV=" 
                + String.format("%.2f%%", currentLTV * 100));
            // In a real implementation, this might trigger emergency liquidation
            // For now, we let it pass through to normal circuit breaker logic
        }

        // Step 4: Check if intervention needed
        if (currentLTV < this.ltvThreshold) {
            return 0.0;  // Healthy, no action needed
        }

        // Step 5: Check circuit breaker conditions
        if (isCircuitBreakerTriggered(time, ethPrice, currentLTV)) {
            return 0.0;  // Circuit breaker active, stop defending
        }

        // Step 6: Check cooldown period
        if (isCooldownActive(time)) {
            logCooldownActive(time);
            return 0.0;  // Too soon since last intervention
        }

        // Step 7: Calculate ETH needed from buffer
        double targetDebt = this.ltvTarget * collateralValue;
        double debtToRepay = states.notionalPrincipal - targetDebt;
        double ethNeeded = debtToRepay / ethPrice;

        // Step 8: Apply per-intervention usage limit
        double maxUsageThisIntervention = this.initialBufferQuantity 
            * this.maxBufferUsagePerIntervention;
        if (ethNeeded > maxUsageThisIntervention) {
            System.out.println("**** BufferLTVModel: Limiting ETH usage to " 
                + String.format("%.4f", maxUsageThisIntervention) 
                + " (max " + (this.maxBufferUsagePerIntervention * 100) + "% per intervention)");
            ethNeeded = maxUsageThisIntervention;
        }

        // Step 9: Check buffer sufficiency
        if (!hasBufferCapacity(ethNeeded)) {
            return 0.0;  // Insufficient buffer
        }

        // Step 10: Execute buffer intervention
        return executeBufferIntervention(ethNeeded, ethPrice, states, time);
    }

    // -------------------------------------------------------------------------
    // Circuit Breaker Logic
    // -------------------------------------------------------------------------

    private boolean isCircuitBreakerTriggered(LocalDateTime time, 
                                               double currentPrice, 
                                               double currentLTV) {
        
        // CB1: Maximum interventions reached
        if (interventionCount >= maxInterventions) {
            System.out.println("**** BufferLTVModel: CIRCUIT BREAKER - Max interventions reached ("
                + interventionCount + " of " + maxInterventions + ")");
            return true;
        }

        // CB2: Buffer below minimum reserve
        if (currentBufferQuantity < minBufferReserve) {
            System.out.println("**** BufferLTVModel: CIRCUIT BREAKER - Buffer below reserve ("
                + String.format("%.4f", currentBufferQuantity) + " < " 
                + String.format("%.4f", minBufferReserve) + " ETH)");
            return true;
        }

        // CB3: Falling knife detection (only after first intervention)
        if (interventionCount >= 1 && isFallingKnife(time, currentPrice)) {
            System.out.println("**** BufferLTVModel: CIRCUIT BREAKER - Falling knife detected");
            System.out.println("  Price dropped " 
                + String.format("%.1f%%", getPriceDropPercentage(currentPrice) * 100)
                + " since first intervention");
            System.out.println("  Stopping buffer usage to preserve remaining reserve");
            return true;
        }

        return false;
    }

    private boolean isFallingKnife(LocalDateTime time, double currentPrice) {
        if (firstInterventionTime == null || priceAtFirstIntervention == 0.0) {
            return false;
        }

        long millisSinceFirst = ChronoUnit.MILLIS.between(firstInterventionTime, time);
        double priceDrop = (priceAtFirstIntervention - currentPrice) / priceAtFirstIntervention;

        return (millisSinceFirst <= fallingKnifeTimeWindowMillis 
                && priceDrop >= fallingKnifePriceDropThreshold);
    }

    private double getPriceDropPercentage(double currentPrice) {
        if (priceAtFirstIntervention == 0.0) return 0.0;
        return (priceAtFirstIntervention - currentPrice) / priceAtFirstIntervention;
    }

    // -------------------------------------------------------------------------
    // Cooldown Logic
    // -------------------------------------------------------------------------

    private boolean isCooldownActive(LocalDateTime time) {
        if (lastInterventionTime == null) {
            return false;  // First intervention, no cooldown
        }

        long millisSinceLast = ChronoUnit.MILLIS.between(lastInterventionTime, time);
        return millisSinceLast < cooldownMillis;
    }

    private void logCooldownActive(LocalDateTime time) {
        long millisSinceLast = ChronoUnit.MILLIS.between(lastInterventionTime, time);
        long millisRemaining = cooldownMillis - millisSinceLast;
        long hoursRemaining = millisRemaining / (1000 * 60 * 60);
        
        System.out.println("**** BufferLTVModel: COOLDOWN active");
        System.out.println("  Last intervention: " + lastInterventionTime);
        System.out.println("  Cooldown remaining: ~" + hoursRemaining + " hours");
    }

    // -------------------------------------------------------------------------
    // Buffer Capacity Check
    // -------------------------------------------------------------------------

    private boolean hasBufferCapacity(double ethNeeded) {
        if (ethNeeded > currentBufferQuantity) {
            System.out.println("**** BufferLTVModel: INSUFFICIENT BUFFER");
            System.out.println("  Needed: " + String.format("%.4f", ethNeeded) + " ETH");
            System.out.println("  Available: " + String.format("%.4f", currentBufferQuantity) + " ETH");
            return false;
        }

        // Also check that we won't go below reserve
        if ((currentBufferQuantity - ethNeeded) < minBufferReserve) {
            System.out.println("**** BufferLTVModel: Would breach minimum reserve");
            System.out.println("  After use: " + String.format("%.4f", currentBufferQuantity - ethNeeded) + " ETH");
            System.out.println("  Min reserve: " + String.format("%.4f", minBufferReserve) + " ETH");
            return false;
        }

        return true;
    }

    // -------------------------------------------------------------------------
    // Intervention Execution
    // -------------------------------------------------------------------------

    private double executeBufferIntervention(double ethNeeded, 
                                             double ethPrice, 
                                             StateSpace states,
                                             LocalDateTime time) {
        
        // Calculate repayment
        double usdcRaised = ethNeeded * ethPrice;
        double repayFraction = usdcRaised / states.notionalPrincipal;

        // Update state BEFORE logging (for accurate reporting)
        currentBufferQuantity -= ethNeeded;
        interventionCount++;
        lastInterventionTime = time;

        // Track first intervention for falling knife detection
        if (firstInterventionTime == null) {
            firstInterventionTime = time;
            priceAtFirstIntervention = ethPrice;
        }

        // Comprehensive logging
        logIntervention(time, ethNeeded, ethPrice, usdcRaised, repayFraction, states);

        // Return clamped repayment fraction
        return Math.max(0.0, Math.min(1.0, repayFraction));
    }

    private void logIntervention(LocalDateTime time, 
                                 double ethSold, 
                                 double ethPrice,
                                 double usdcRaised, 
                                 double repayFraction,
                                 StateSpace states) {
        
        System.out.println("════════════════════════════════════════════════════════════");
        System.out.println("**** BufferLTVModel INTERVENTION ****");
        System.out.println("════════════════════════════════════════════════════════════");
        System.out.println("  Time: " + time);
        System.out.println("  Intervention: " + interventionCount + " of " + maxInterventions);
        System.out.println("  ");
        System.out.println("  BUFFER USAGE:");
        System.out.println("    ETH price: $" + String.format("%.2f", ethPrice));
        System.out.println("    ETH sold from buffer: " + String.format("%.4f", ethSold) + " ETH");
        System.out.println("    USDC raised: $" + String.format("%.2f", usdcRaised));
        System.out.println("    Buffer before: " + String.format("%.4f", currentBufferQuantity + ethSold) + " ETH");
        System.out.println("    Buffer after: " + String.format("%.4f", currentBufferQuantity) + " ETH");
        System.out.println("    Buffer used: " + String.format("%.1f%%", 
            (ethSold / initialBufferQuantity) * 100) + " of original");
        System.out.println("  ");
        System.out.println("  LOAN REPAYMENT:");
        System.out.println("    Loan before: $" + String.format("%.2f", states.notionalPrincipal));
        System.out.println("    Repay amount: $" + String.format("%.2f", usdcRaised));
        System.out.println("    Repay fraction: " + String.format("%.4f", repayFraction));
        System.out.println("    Loan after: $" + String.format("%.2f", 
            states.notionalPrincipal * (1 - repayFraction)));
        System.out.println("  ");
        System.out.println("  COLLATERAL (PRESERVED):");
        System.out.println("    Quantity: " + String.format("%.4f", collateralQuantity) + " ETH (UNCHANGED)");
        double newLTV = (states.notionalPrincipal * (1 - repayFraction)) 
                       / (collateralQuantity * ethPrice);
        System.out.println("    New LTV: " + String.format("%.2f%%", newLTV * 100));
        System.out.println("  ");
        System.out.println("  CIRCUIT BREAKER:");
        System.out.println("    Status: " + (interventionCount >= maxInterventions 
            ? "TRIGGERED (no more interventions)" 
            : "ACTIVE (" + (maxInterventions - interventionCount) + " remaining)"));
        System.out.println("    Buffer reserve: " + String.format("%.4f", minBufferReserve) + " ETH");
        System.out.println("    Cooldown until: " + time.plus(cooldownMillis, ChronoUnit.MILLIS));
        System.out.println("════════════════════════════════════════════════════════════");
    }
}
