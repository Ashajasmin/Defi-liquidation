package org.actus.risksrv3.utils.defiliquidation1;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.defiliquidation1.CascadeProbabilityModelData;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;
import org.actus.risksrv3.utils.MultiMarketRiskModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * CascadeProbabilityModel (Domain 1 — Model 2.6)
 *
 * Models the feedback loop where liquidations cause price drops, which trigger
 * more liquidations. Estimates cascade probability based on:
 *   - Current pool-level aggregate LTV distribution
 *   - Market depth (how much sell pressure the market can absorb)
 *   - Position size relative to market liquidity
 *
 * When cascade probability is high, recommends defensive strategies:
 *   - Strategy C (Deleveraging) instead of Strategy D (Controlled Liquidation)
 *   - Pre-emptive partial repayment to exit the cascade zone
 *
 * Scholarly basis:
 *   - Lehar & Parlour (2022), BIS WP 1062: Liquidation cascades in DeFi
 *   - Heimbach & Huang (2024), BIS WP 1171: Voluntary buffer behavior
 *   - Karagiannis & Arvanitis (2025): Analytical liquidation probability
 *
 * Data sources:
 *   - DeFi Llama API: TVL and protocol utilization data
 *   - Glassnode: ETH exchange reserves, net flow (behavioral sell pressure)
 *   - CoinGecko: order book depth proxy via volume data
 *   - POOL_AGG_LTV reference index: aggregate pool LTV distribution
 *   - MARKET_DEPTH reference index: market depth in USD terms
 */
public class CascadeProbabilityModel implements BehaviorRiskModelProvider {

    public static final String CALLOUT_TYPE = "MRD";

    private final String riskFactorId;
    private final String collateralPriceMOC;
    private final String poolAggLtvMOC;        // aggregate pool LTV metric
    private final String marketDepthMOC;       // market depth in USD
    private final double collateralQuantity;
    private final double positionValueUSD;     // approximate position size
    private final double cascadeThreshold;     // probability above which to act (e.g. 0.60)
    private final double priceImpactFactor;    // estimated price impact per $1M sold
    private final double defensiveRepayFraction; // e.g. 0.20 when cascade risk high
    private final List<String> monitoringEventTimes;
    private final MultiMarketRiskModel marketModel;

    public CascadeProbabilityModel(String riskFactorId,
                                   CascadeProbabilityModelData data,
                                   MultiMarketRiskModel marketModel) {
        this.riskFactorId          = riskFactorId;
        this.collateralPriceMOC    = data.getCollateralPriceMOC();
        this.poolAggLtvMOC         = data.getPoolAggLtvMOC();
        this.marketDepthMOC        = data.getMarketDepthMOC();
        this.collateralQuantity    = data.getCollateralQuantity();
        this.positionValueUSD      = data.getPositionValueUSD();
        this.cascadeThreshold      = data.getCascadeThreshold();
        this.priceImpactFactor     = data.getPriceImpactFactor();
        this.defensiveRepayFraction= data.getDefensiveRepayFraction();
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
                    System.out.println("**** CascadeProbabilityModel: SKIPPING pre-IED callout " + eventTime + " (IED=" + ied + ")");
                    continue;
                }
            }
            callouts.add(new CalloutData(this.riskFactorId, eventTime, CALLOUT_TYPE));
        }
        return callouts;
    }

    @Override
    public double stateAt(String id, LocalDateTime time, StateSpace states) {

        double ethPrice = this.marketModel.stateAt(this.collateralPriceMOC, time);
        double poolAggLtv = this.marketModel.stateAt(this.poolAggLtvMOC, time);
        double marketDepth = this.marketModel.stateAt(this.marketDepthMOC, time);

        if (ethPrice <= 0.0 || marketDepth <= 0.0) return 0.0;

        // Estimate cascade probability
        // Factor 1: Pool-level stress (higher aggregate LTV = more positions near liquidation)
        double poolStressFactor = Math.max(0.0, (poolAggLtv - 0.50) / 0.30);
        poolStressFactor = Math.min(1.0, poolStressFactor);

        // Factor 2: Market depth vs position size
        // If our position is large relative to available liquidity, liquidation will move price
        double collateralValueUSD = collateralQuantity * ethPrice;
        double depthRatio = collateralValueUSD / marketDepth;
        double depthFactor = Math.min(1.0, depthRatio * 2.0); // scale: 50% of depth = factor 1.0

        // Factor 3: Price impact feedback loop
        double estimatedPriceImpact = collateralValueUSD * priceImpactFactor / 1_000_000.0;
        double impactFactor = Math.min(1.0, estimatedPriceImpact / ethPrice);

        // Combined cascade probability (weighted average)
        double cascadeProb = 0.40 * poolStressFactor
                           + 0.35 * depthFactor
                           + 0.25 * impactFactor;
        cascadeProb = Math.min(1.0, cascadeProb);

        System.out.println("**** CascadeProbabilityModel: time=" + time
                + " poolAggLtv=" + String.format("%.4f", poolAggLtv)
                + " marketDepth=" + String.format("%.0f", marketDepth)
                + " cascadeProb=" + String.format("%.4f", cascadeProb)
                + " poolStress=" + String.format("%.4f", poolStressFactor)
                + " depthFactor=" + String.format("%.4f", depthFactor));

        if (cascadeProb >= this.cascadeThreshold) {
            System.out.println("**** CascadeProbabilityModel: CASCADE_RISK HIGH — defensive repay");
            return defensiveRepayFraction;
        }
        return 0.0;
    }
}
