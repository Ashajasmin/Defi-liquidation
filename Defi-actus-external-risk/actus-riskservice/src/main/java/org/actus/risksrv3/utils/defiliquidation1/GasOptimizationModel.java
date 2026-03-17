package org.actus.risksrv3.utils.defiliquidation1;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.defiliquidation1.GasOptimizationModelData;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;
import org.actus.risksrv3.utils.MultiMarketRiskModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * GasOptimizationModel (Domain 1 — Model 2.7)
 *
 * Adjusts liquidation and intervention thresholds dynamically based on
 * Ethereum gas costs. When gas is high, small interventions (top-ups,
 * partial repayments) may cost more than they save — creating "zombie positions."
 *
 * Decision logic:
 *   gasCostUSD = gasPrice * gasUnits * ethPrice / 1e9
 *   interventionBenefit = potential savings from reducing LTV
 *   netBenefit = interventionBenefit - gasCostUSD
 *
 *   netBenefit > 0  → proceed with intervention (return repay fraction)
 *   netBenefit <= 0 → defer intervention (return 0.0) unless critical
 *
 * Data sources:
 *   - ETH_GAS_PRICE reference index (Gwei): from Etherscan Gas Tracker API (free)
 *   - CoinGecko: ETH price for gas cost conversion to USD
 *   - EIP-1559 base fee from Ethereum JSON-RPC (free via Infura/Alchemy free tiers)
 *
 * Sadeghi (2025): "Transaction fee role in liquidation dynamics" — validates
 * that gas costs significantly affect liquidation timing and profitability.
 */
public class GasOptimizationModel implements BehaviorRiskModelProvider {

    public static final String CALLOUT_TYPE = "MRD";

    private final String riskFactorId;
    private final String collateralPriceMOC;
    private final String gasPriceMOC;          // gas price in Gwei as reference index
    private final double collateralQuantity;
    private final double gasUnitsPerTx;        // estimated gas units (e.g. 250000 for liquidation)
    private final double ltvThreshold;         // base LTV threshold before gas adjustment
    private final double liquidationThreshold;
    private final double ltvTarget;
    private final double minBenefitUSD;        // minimum net benefit to proceed (e.g. $50)
    private final List<String> monitoringEventTimes;
    private final MultiMarketRiskModel marketModel;

    public GasOptimizationModel(String riskFactorId,
                                GasOptimizationModelData data,
                                MultiMarketRiskModel marketModel) {
        this.riskFactorId          = riskFactorId;
        this.collateralPriceMOC    = data.getCollateralPriceMOC();
        this.gasPriceMOC           = data.getGasPriceMOC();
        this.collateralQuantity    = data.getCollateralQuantity();
        this.gasUnitsPerTx         = data.getGasUnitsPerTx();
        this.ltvThreshold          = data.getLtvThreshold();
        this.liquidationThreshold  = data.getLiquidationThreshold();
        this.ltvTarget             = data.getLtvTarget();
        this.minBenefitUSD         = data.getMinBenefitUSD();
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
                    System.out.println("**** GasOptimizationModel: SKIPPING pre-IED callout " + eventTime + " (IED=" + ied + ")");
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
        double gasPriceGwei = this.marketModel.stateAt(this.gasPriceMOC, time);

        if (ethPrice <= 0.0) return 0.0;

        // Compute gas cost in USD
        double gasCostETH = (gasPriceGwei * gasUnitsPerTx) / 1e9;
        double gasCostUSD = gasCostETH * ethPrice;

        // Compute LTV
        double collateralValue = collateralQuantity * ethPrice;
        double debt = states.notionalPrincipal + states.accruedInterest;
        if (debt <= 0.0) return 0.0;
        double currentLTV = debt / collateralValue;

        System.out.println("**** GasOptimizationModel: time=" + time
                + " gasGwei=" + String.format("%.1f", gasPriceGwei)
                + " gasCostUSD=" + String.format("%.2f", gasCostUSD)
                + " LTV=" + String.format("%.4f", currentLTV));

        // Critical override: always liquidate regardless of gas if past liquidation threshold
        if (currentLTV >= liquidationThreshold) {
            System.out.println("**** GasOptimizationModel: CRITICAL_LIQUIDATION (ignoring gas)");
            return 1.0;
        }

        // Gas-aware intervention decision
        if (currentLTV >= ltvThreshold) {
            double targetDebt = ltvTarget * collateralValue;
            double repayAmount = debt - targetDebt;
            double repayFraction = repayAmount / debt;

            // Estimate benefit: avoided liquidation penalty (typically 5-10% of collateral)
            double potentialPenalty = collateralValue * 0.05;
            double netBenefit = potentialPenalty - gasCostUSD;

            System.out.println("**** GasOptimizationModel: repayAmount="
                    + String.format("%.2f", repayAmount)
                    + " potentialPenalty=" + String.format("%.2f", potentialPenalty)
                    + " gasCost=" + String.format("%.2f", gasCostUSD)
                    + " netBenefit=" + String.format("%.2f", netBenefit));

            if (netBenefit >= minBenefitUSD) {
                return Math.max(0.0, Math.min(1.0, repayFraction));
            } else {
                System.out.println("**** GasOptimizationModel: DEFER (gas too expensive)");
                return 0.0;
            }
        }
        return 0.0;
    }
}
