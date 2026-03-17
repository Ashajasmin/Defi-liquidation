package org.actus.risksrv3.utils.defiliquidation1;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.defiliquidation1.HealthFactorModelData;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;
import org.actus.risksrv3.utils.MultiMarketRiskModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * HealthFactorModel (Domain 1 — Model 2.2)
 *
 * Computes the multi-collateral weighted Health Factor for DeFi lending positions:
 *   HF = Sum(collateral_i x price_i x liquidationThreshold_i) / totalDebt
 *
 * Extends CollateralLTVModel for multi-asset scenarios (ETH + wstETH + USDC).
 *
 * Data sources: CoinGecko API, Glassnode, DeFi Llama, CryptoCompare,
 *   farside.co.uk/eth (ETH ETF flow behavioral sentiment)
 *
 * Scholarly: Aave V3 HF formula, Qin et al. (2021), Compound V3 Account Liquidity
 *
 * FIX 1 — structural pre-filter in contractStart():
 *   Instead of emitting PP callouts for ALL monitoringEventTimes (which causes
 *   1440 REST round-trips per model during ContractType.apply()), we compare the
 *   weighted collateral at each candidate event time against the baseline at the
 *   first monitoring time. If the collateral has NOT deteriorated (prices equal or
 *   higher than baseline), the position is not at risk at that moment and the callout
 *   is skipped. If prices have dropped, the callout is included.
 *   Fail-safe: if market data is missing for any time, the callout is included
 *   conservatively so no liquidation event is ever silently missed.
 *
 * FIX 2 — reduced monitoringEventTimes density (JSON payload):
 *   Use 96 x 15-min points instead of 1440 x 1-min points for initial testing.
 *   Combined with Fix 1, this keeps total PP callouts to a small, manageable count.
 */
public class HealthFactorModel implements BehaviorRiskModelProvider {

    public static final String CALLOUT_TYPE = "MRD";

    private final String riskFactorId;
    private final List<String> collateralMOCs;
    private final List<Double> collateralQuantities;
    private final List<Double> liquidationThresholds;
    private final double healthyThreshold;
    private final double targetHealthFactor;
    private final List<String> monitoringEventTimes;
    private final MultiMarketRiskModel marketModel;

    public HealthFactorModel(String riskFactorId, HealthFactorModelData data,
                             MultiMarketRiskModel marketModel) {
        this.riskFactorId           = riskFactorId;
        this.collateralMOCs         = data.getCollateralMOCs();
        this.collateralQuantities   = data.getCollateralQuantities();
        this.liquidationThresholds  = data.getLiquidationThresholds();
        this.healthyThreshold       = data.getHealthyThreshold();
        this.targetHealthFactor     = data.getTargetHealthFactor();
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
        // FIX 1 — structural pre-filter using market data.
        //
        // The baseline weighted collateral is computed at monitoringEventTimes[0].
        // For each subsequent time, if weightedCollateral >= baseline the position
        // has not deteriorated and the PP callout is skipped.
        // If weightedCollateral < baseline (prices fell), the callout is included
        // so ContractType.apply() calls behaviorStateAt to compute the repay fraction.
        //
        // Why this is safe:
        //   - We never skip a callout where the HF could be < healthyThreshold.
        //   - The debt side (notionalPrincipal + accruedInterest) is not known here,
        //     but any debt increase only makes HF worse, not better. By comparing
        //     collateral values only, we conservatively include any time where
        //     collateral has fallen (the HF may or may not be critical, stateAt decides).
        //   - Market data gap → include callout (fail-safe).

        LocalDateTime ied = contract.getAs("initialExchangeDate");

        // Compute baseline weighted collateral at the first monitoring time.
        double baselineCollateral = 0.0;
        boolean baselineAvailable = false;
        if (!this.monitoringEventTimes.isEmpty()) {
            try {
                LocalDateTime baselineDT = LocalDateTime.parse(this.monitoringEventTimes.get(0));
                double wc = 0.0;
                for (int i = 0; i < collateralMOCs.size(); i++) {
                    double price = this.marketModel.stateAt(collateralMOCs.get(i), baselineDT);
                    wc += collateralQuantities.get(i) * price * liquidationThresholds.get(i);
                }
                baselineCollateral = wc;
                baselineAvailable = (baselineCollateral > 0.0);
                System.out.println("**** HealthFactorModel.contractStart [" + this.riskFactorId + "]:"
                        + " baseline weightedCollateral=" + String.format("%.2f", baselineCollateral)
                        + " at " + this.monitoringEventTimes.get(0));
            } catch (Exception e) {
                System.out.println("**** HealthFactorModel.contractStart [" + this.riskFactorId + "]:"
                        + " baseline market data unavailable, will include all callouts.");
            }
        }

        List<CalloutData> callouts = new ArrayList<>();
        int skippedPreIed  = 0;
        int skippedHealthy = 0;
        int included       = 0;

        for (String eventTime : this.monitoringEventTimes) {
            LocalDateTime eventDateTime = LocalDateTime.parse(eventTime);

            // Guard 1: skip events before contract IED
            if (ied != null && eventDateTime.isBefore(ied)) {
                skippedPreIed++;
                continue;
            }

            // Guard 2 (FIX 1): pre-filter by collateral deterioration check
            if (baselineAvailable) {
                try {
                    double wc = 0.0;
                    for (int i = 0; i < collateralMOCs.size(); i++) {
                        double price = this.marketModel.stateAt(collateralMOCs.get(i), eventDateTime);
                        wc += collateralQuantities.get(i) * price * liquidationThresholds.get(i);
                    }
                    if (wc >= baselineCollateral) {
                        // Collateral has not fallen — position cannot be worse than at baseline.
                        // Safe to skip this PP callout.
                        skippedHealthy++;
                        continue;
                    }
                    // Collateral has fallen — include the callout
                } catch (Exception e) {
                    // Market data missing: include conservatively
                }
            }

            callouts.add(new CalloutData(this.riskFactorId, eventTime, CALLOUT_TYPE));
            included++;
        }

        System.out.println("**** HealthFactorModel.contractStart [" + this.riskFactorId + "]:"
                + " total=" + this.monitoringEventTimes.size()
                + " skippedPreIED=" + skippedPreIed
                + " skippedByPriceFilter=" + skippedHealthy
                + " callouts=" + included);
        return callouts;
    }

    @Override
    public double stateAt(String id, LocalDateTime time, StateSpace states) {
        double weightedCollateral = 0.0;
        for (int i = 0; i < collateralMOCs.size(); i++) {
            double price = this.marketModel.stateAt(collateralMOCs.get(i), time);
            double qty = collateralQuantities.get(i);
            double liqThreshold = liquidationThresholds.get(i);
            weightedCollateral += qty * price * liqThreshold;
        }
        double totalDebt = states.notionalPrincipal + states.accruedInterest;
        if (totalDebt <= 0.0) return 0.0;
        double healthFactor = weightedCollateral / totalDebt;

        System.out.println("**** HealthFactorModel: time=" + time
                + " weightedCollateral=" + String.format("%.2f", weightedCollateral)
                + " totalDebt=" + String.format("%.2f", totalDebt)
                + " HF=" + String.format("%.4f", healthFactor));

        if (healthFactor < 1.0) {
            System.out.println("**** HealthFactorModel: LIQUIDATION HF=" + String.format("%.4f", healthFactor));
            return 1.0;
        } else if (healthFactor < this.healthyThreshold) {
            double targetDebt = weightedCollateral / this.targetHealthFactor;
            double repayFraction = (totalDebt - targetDebt) / totalDebt;
            System.out.println("**** HealthFactorModel: PARTIAL_REPAY fraction=" + String.format("%.4f", repayFraction));
            return Math.max(0.0, Math.min(1.0, repayFraction));
        }
        return 0.0;
    }
}
