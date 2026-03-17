package org.actus.risksrv3.utils.defiliquidation1;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.defiliquidation1.CollateralRebalancingModelData;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;
import org.actus.risksrv3.utils.MultiMarketRiskModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * CollateralRebalancingModel (Domain 1 — Model 2.4)
 *
 * For hybrid collateral positions (ETH + USDC + tokenized invoice), this model
 * monitors individual asset performance and recommends rebalancing when one asset
 * crashes while others remain stable.
 *
 * This is the core model for CHRONOS-SHIELD's 5 intervention strategies:
 *   Strategy A: Collateral Top-Up (deposit more USDC)
 *   Strategy B: Partial Debt Repayment
 *   Strategy C: Position Deleveraging
 *   Strategy D: Controlled Liquidation with Buyback
 *   Strategy E: Invoice Sacrifice (release invoice collateral to preserve ETH)
 *
 * The model computes per-asset contribution to overall collateral health and
 * recommends which strategy to use based on relative asset performance.
 *
 * Incorporates ETH ETF flow data as behavioral sentiment signal:
 *   - Large ETF outflows → bearish signal → more aggressive rebalancing
 *   - Large ETF inflows → bullish signal → relaxed thresholds
 *   Source: farside.co.uk/eth (ETH ETF flow data, updated daily)
 *
 * Data sources:
 *   - CoinGecko: ETH, BTC, stablecoin prices
 *   - ETH ETF flow data (ETH_ETF_FLOW reference index): net daily flow in $M
 *   - DeFi Llama: protocol-level collateral composition data
 */
public class CollateralRebalancingModel implements BehaviorRiskModelProvider {

    public static final String CALLOUT_TYPE = "MRD";

    private final String riskFactorId;
    private final String volatileAssetMOC;     // e.g. "ETH_USD"
    private final String stableAssetMOC;       // e.g. "USDC_USD"
    private final String etfFlowMOC;           // e.g. "ETH_ETF_FLOW" (net daily $M)
    private final double volatileAssetQty;     // e.g. 2.5 ETH
    private final double stableAssetQty;       // e.g. 1000 USDC
    private final double invoiceValueUSD;      // e.g. 5000 USD (tokenized invoice)
    private final double overallLtvThreshold;  // e.g. 0.75
    private final double liquidationThreshold; // e.g. 0.83
    private final double ltvTarget;            // e.g. 0.65
    private final double etfFlowThreshold;     // e.g. -100 ($100M outflow triggers tightening)
    private final double etfSensitivity;       // e.g. 0.05 (5% threshold tightening per $100M outflow)
    private final List<String> monitoringEventTimes;
    private final MultiMarketRiskModel marketModel;

    public CollateralRebalancingModel(String riskFactorId,
                                      CollateralRebalancingModelData data,
                                      MultiMarketRiskModel marketModel) {
        this.riskFactorId          = riskFactorId;
        this.volatileAssetMOC      = data.getVolatileAssetMOC();
        this.stableAssetMOC        = data.getStableAssetMOC();
        this.etfFlowMOC            = data.getEtfFlowMOC();
        this.volatileAssetQty      = data.getVolatileAssetQty();
        this.stableAssetQty        = data.getStableAssetQty();
        this.invoiceValueUSD       = data.getInvoiceValueUSD();
        this.overallLtvThreshold   = data.getOverallLtvThreshold();
        this.liquidationThreshold  = data.getLiquidationThreshold();
        this.ltvTarget             = data.getLtvTarget();
        this.etfFlowThreshold      = data.getEtfFlowThreshold();
        this.etfSensitivity        = data.getEtfSensitivity();
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
                    System.out.println("**** CollateralRebalancingModel: SKIPPING pre-IED callout " + eventTime + " (IED=" + ied + ")");
                    continue;
                }
            }
            callouts.add(new CalloutData(this.riskFactorId, eventTime, CALLOUT_TYPE));
        }
        return callouts;
    }

    @Override
    public double stateAt(String id, LocalDateTime time, StateSpace states) {

        double ethPrice = this.marketModel.stateAt(this.volatileAssetMOC, time);
        double usdcPrice = this.marketModel.stateAt(this.stableAssetMOC, time);
        double etfNetFlow = this.marketModel.stateAt(this.etfFlowMOC, time);

        if (ethPrice <= 0.0) return 0.0;

        // Compute collateral components
        double ethValue = volatileAssetQty * ethPrice;
        double stableValue = stableAssetQty * usdcPrice;
        double totalCollateral = ethValue + stableValue + invoiceValueUSD;

        double debt = states.notionalPrincipal + states.accruedInterest;
        if (debt <= 0.0) return 0.0;

        // Adjust thresholds based on ETH ETF flow sentiment
        double adjustedLtvThreshold = overallLtvThreshold;
        double adjustedLiqThreshold = liquidationThreshold;

        if (etfNetFlow < etfFlowThreshold) {
            // Negative flow (outflows) → tighten thresholds
            double flowExcess = (etfFlowThreshold - etfNetFlow) / Math.abs(etfFlowThreshold);
            double tightening = etfSensitivity * flowExcess;
            adjustedLtvThreshold = overallLtvThreshold - tightening;
            adjustedLiqThreshold = liquidationThreshold - tightening;
        }

        double currentLTV = debt / totalCollateral;

        // Compute volatile asset contribution ratio
        double volatileRatio = ethValue / totalCollateral;

        System.out.println("**** CollateralRebalancingModel: time=" + time
                + " ethPrice=" + String.format("%.2f", ethPrice)
                + " ethValue=" + String.format("%.2f", ethValue)
                + " stableValue=" + String.format("%.2f", stableValue)
                + " invoiceValue=" + String.format("%.2f", invoiceValueUSD)
                + " totalCollateral=" + String.format("%.2f", totalCollateral)
                + " LTV=" + String.format("%.4f", currentLTV)
                + " volatileRatio=" + String.format("%.4f", volatileRatio)
                + " etfFlow=" + String.format("%.1f", etfNetFlow)
                + " adjLtvThresh=" + String.format("%.4f", adjustedLtvThreshold));

        // Decision logic using adjusted thresholds
        if (currentLTV >= adjustedLiqThreshold) {
            System.out.println("**** CollateralRebalancingModel: LIQUIDATION");
            return 1.0;
        } else if (currentLTV >= adjustedLtvThreshold) {
            double targetDebt = ltvTarget * totalCollateral;
            double repayFraction = (debt - targetDebt) / debt;

            // Strategy selection based on volatile ratio
            if (volatileRatio > 0.80) {
                // Mostly volatile — Strategy B: aggressive partial repayment
                System.out.println("**** CollateralRebalancingModel: STRATEGY_B partial_repay="
                        + String.format("%.4f", repayFraction));
            } else if (volatileRatio > 0.50) {
                // Mixed — rebalance by shifting composition
                System.out.println("**** CollateralRebalancingModel: STRATEGY_A/C rebalance");
            } else {
                // Mostly stable — invoice sacrifice may be appropriate
                System.out.println("**** CollateralRebalancingModel: STRATEGY_E invoice_sacrifice");
            }
            return Math.max(0.0, Math.min(1.0, repayFraction));
        }
        return 0.0;
    }
}
