package org.actus.risksrv3.utils.defiliquidation1;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.defiliquidation1.CorrelationRiskModelData;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;
import org.actus.risksrv3.utils.MultiMarketRiskModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * CorrelationRiskModel (Domain 1 — Model 2.5)
 *
 * Monitors rolling correlation between collateral assets (e.g., ETH and BTC).
 * During normal markets, BTC/ETH correlation ~0.6-0.8 provides diversification.
 * During crashes, correlation spikes to ~0.95+, destroying diversification benefit.
 *
 * When correlation exceeds threshold, the model adjusts effective collateral value
 * downward to reflect true risk, potentially triggering early intervention.
 *
 * Data sources:
 *   - CoinGecko API: historical daily prices for correlation computation
 *   - CryptoCompare: tick-level data for intraday correlation
 *   - Glassnode: on-chain correlation metrics between BTC/ETH
 *   - ETH DVOL (Deribit): implied vol correlation with BTC DVOL
 *
 * Historical correlation data (ETH vs BTC):
 *   Normal markets:     0.60-0.80 rolling 30-day
 *   Moderate stress:    0.80-0.90
 *   High stress:        0.90-0.95
 *   Extreme stress:     0.95-1.00 (Mar 2020, May 2022, Nov 2022)
 */
public class CorrelationRiskModel implements BehaviorRiskModelProvider {

    public static final String CALLOUT_TYPE = "MRD";

    private final String riskFactorId;
    private final String asset1MOC;                // e.g. "ETH_USD"
    private final String asset2MOC;                // e.g. "BTC_USD"
    private final double asset1Quantity;
    private final double asset2Quantity;
    private final double correlationThreshold;     // e.g. 0.90
    private final double diversificationHaircut;   // e.g. 0.15 (15% reduction when correlated)
    private final double baseLtvThreshold;         // e.g. 0.75
    private final double liquidationThreshold;     // e.g. 0.83
    private final int rollingWindowSize;           // e.g. 10
    private final List<String> monitoringEventTimes;
    private final MultiMarketRiskModel marketModel;

    // Rolling price history for correlation computation
    private final LinkedList<double[]> priceHistory = new LinkedList<>(); // [0]=price1, [1]=price2

    public CorrelationRiskModel(String riskFactorId,
                                CorrelationRiskModelData data,
                                MultiMarketRiskModel marketModel) {
        this.riskFactorId           = riskFactorId;
        this.asset1MOC              = data.getAsset1MOC();
        this.asset2MOC              = data.getAsset2MOC();
        this.asset1Quantity         = data.getAsset1Quantity();
        this.asset2Quantity         = data.getAsset2Quantity();
        this.correlationThreshold   = data.getCorrelationThreshold();
        this.diversificationHaircut = data.getDiversificationHaircut();
        this.baseLtvThreshold       = data.getBaseLtvThreshold();
        this.liquidationThreshold   = data.getLiquidationThreshold();
        this.rollingWindowSize      = data.getRollingWindowSize();
        this.monitoringEventTimes   = data.getMonitoringEventTimes();
        this.marketModel            = marketModel;
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
                    System.out.println("**** CorrelationRiskModel: SKIPPING pre-IED callout " + eventTime + " (IED=" + ied + ")");
                    continue;
                }
            }
            callouts.add(new CalloutData(this.riskFactorId, eventTime, CALLOUT_TYPE));
        }
        return callouts;
    }

    @Override
    public double stateAt(String id, LocalDateTime time, StateSpace states) {

        double price1 = this.marketModel.stateAt(this.asset1MOC, time);
        double price2 = this.marketModel.stateAt(this.asset2MOC, time);

        // Store prices in rolling window
        priceHistory.addLast(new double[]{price1, price2});
        if (priceHistory.size() > rollingWindowSize) {
            priceHistory.removeFirst();
        }

        // Compute rolling Pearson correlation from returns
        double correlation = computeRollingCorrelation();

        // Compute effective collateral (with haircut if highly correlated)
        double rawCollateral = (asset1Quantity * price1) + (asset2Quantity * price2);
        double effectiveCollateral = rawCollateral;

        if (correlation > this.correlationThreshold) {
            // Apply diversification haircut proportional to excess correlation
            double excessCorr = (correlation - this.correlationThreshold)
                    / (1.0 - this.correlationThreshold);
            double haircut = this.diversificationHaircut * excessCorr;
            effectiveCollateral = rawCollateral * (1.0 - haircut);
        }

        double debt = states.notionalPrincipal + states.accruedInterest;
        if (debt <= 0.0 || effectiveCollateral <= 0.0) return 0.0;

        double effectiveLTV = debt / effectiveCollateral;

        System.out.println("**** CorrelationRiskModel: time=" + time
                + " corr=" + String.format("%.4f", correlation)
                + " rawCollateral=" + String.format("%.2f", rawCollateral)
                + " effectiveCollateral=" + String.format("%.2f", effectiveCollateral)
                + " effectiveLTV=" + String.format("%.4f", effectiveLTV));

        if (effectiveLTV >= this.liquidationThreshold) {
            System.out.println("**** CorrelationRiskModel: LIQUIDATION (correlation-adjusted)");
            return 1.0;
        } else if (effectiveLTV >= this.baseLtvThreshold) {
            double targetDebt = 0.65 * effectiveCollateral;
            double repayFraction = (debt - targetDebt) / debt;
            System.out.println("**** CorrelationRiskModel: REBALANCE fraction="
                    + String.format("%.4f", repayFraction));
            return Math.max(0.0, Math.min(1.0, repayFraction));
        }
        return 0.0;
    }

    /**
     * Computes Pearson correlation from rolling price returns.
     */
    private double computeRollingCorrelation() {
        if (priceHistory.size() < 3) return 0.0;

        List<Double> returns1 = new ArrayList<>();
        List<Double> returns2 = new ArrayList<>();

        double[] prev = priceHistory.getFirst();
        for (int i = 1; i < priceHistory.size(); i++) {
            double[] curr = priceHistory.get(i);
            if (prev[0] > 0 && prev[1] > 0) {
                returns1.add(Math.log(curr[0] / prev[0]));
                returns2.add(Math.log(curr[1] / prev[1]));
            }
            prev = curr;
        }

        if (returns1.size() < 2) return 0.0;

        double mean1 = returns1.stream().mapToDouble(d -> d).average().orElse(0.0);
        double mean2 = returns2.stream().mapToDouble(d -> d).average().orElse(0.0);

        double cov = 0.0, var1 = 0.0, var2 = 0.0;
        for (int i = 0; i < returns1.size(); i++) {
            double d1 = returns1.get(i) - mean1;
            double d2 = returns2.get(i) - mean2;
            cov += d1 * d2;
            var1 += d1 * d1;
            var2 += d2 * d2;
        }

        if (var1 == 0.0 || var2 == 0.0) return 0.0;
        return cov / Math.sqrt(var1 * var2);
    }
}
