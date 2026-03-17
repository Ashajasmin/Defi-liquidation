package org.actus.risksrv3.utils.hybridtreasury1;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.hybridtreasury1.YieldArbitrageModelData;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;
import org.actus.risksrv3.utils.MultiMarketRiskModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * YieldArbitrageModel (Domain 4 — Treasury Model 5.5)
 *
 * Computes risk-adjusted yield across all available sources. When best
 * available yield exceeds current position's yield by > minSpreadBps,
 * signals reallocation.
 *
 * ACTUS contract type: PAM/STK/CLM (any yield-bearing position)
 * Market Object Codes consumed:
 *   TBILL_YIELD, ETH_STAKING_YIELD, USDC_LENDING_YIELD
 */
public class YieldArbitrageModel implements BehaviorRiskModelProvider {

    public static final String CALLOUT_TYPE = "MRD";

    private final String riskFactorId;
    private final String tbillYieldMOC;
    private final String stakingYieldMOC;
    private final String lendingYieldMOC;
    private final double minSpreadBps;
    private final double riskAdjustmentFactor;
    private final List<String> monitoringEventTimes;
    private final MultiMarketRiskModel marketModel;

    public YieldArbitrageModel(String riskFactorId,
                               YieldArbitrageModelData data,
                               MultiMarketRiskModel marketModel) {
        this.riskFactorId         = riskFactorId;
        this.tbillYieldMOC        = data.getTbillYieldMOC();
        this.stakingYieldMOC      = data.getStakingYieldMOC();
        this.lendingYieldMOC      = data.getLendingYieldMOC();
        this.minSpreadBps         = data.getMinSpreadBps();
        this.riskAdjustmentFactor = data.getRiskAdjustmentFactor();
        this.monitoringEventTimes = data.getMonitoringEventTimes();
        this.marketModel          = marketModel;
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
            if (ied != null) { LocalDateTime edt = LocalDateTime.parse(eventTime); if (edt.isBefore(ied)) { System.out.println("**** YieldArbitrageModel: SKIPPING pre-IED callout " + eventTime + " (IED=" + ied + ")"); continue; } }
            callouts.add(new CalloutData(this.riskFactorId, eventTime, CALLOUT_TYPE));
        }
        return callouts;
    }

    @Override
    public double stateAt(String id, LocalDateTime time, StateSpace states) {

        double tbillYield   = this.marketModel.stateAt(this.tbillYieldMOC, time);
        double stakingYield = this.marketModel.stateAt(this.stakingYieldMOC, time);
        double lendingYield = this.marketModel.stateAt(this.lendingYieldMOC, time);

        double currentYield = states.nominalInterestRate;

        double adjTbill   = tbillYield;
        double adjStaking = stakingYield * this.riskAdjustmentFactor;
        double adjLending = lendingYield * this.riskAdjustmentFactor;

        double bestAlternative = Math.max(adjTbill, Math.max(adjStaking, adjLending));
        String bestSource;
        if (bestAlternative == adjTbill) bestSource = "T-BILL";
        else if (bestAlternative == adjStaking) bestSource = "ETH_STAKING";
        else bestSource = "USDC_LENDING";

        double spreadDecimal = bestAlternative - currentYield;
        double spreadBps = spreadDecimal * 10000.0;

        System.out.println("**** YieldArbitrageModel: time=" + time
                + " currentYield=" + String.format("%.4f", currentYield)
                + " tbill=" + String.format("%.4f", tbillYield)
                + " staking=" + String.format("%.4f", stakingYield)
                + " lending=" + String.format("%.4f", lendingYield)
                + " bestAdj=" + String.format("%.4f", bestAlternative)
                + " (" + bestSource + ")"
                + " spreadBps=" + String.format("%.1f", spreadBps));

        if (spreadBps > this.minSpreadBps) {
            System.out.println("**** YieldArbitrageModel: OPPORTUNITY"
                    + " spreadBps=" + String.format("%.1f", spreadBps)
                    + " > " + minSpreadBps
                    + " → reallocate to " + bestSource);
            return Math.min(1.0, spreadDecimal);
        }

        return 0.0;
    }
}
