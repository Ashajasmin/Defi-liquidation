package org.actus.risksrv3.utils.hybridtreasury1;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.hybridtreasury1.LiquidityBufferModelData;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;
import org.actus.risksrv3.utils.MultiMarketRiskModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * LiquidityBufferModel (Domain 4 — Treasury Model 5.2)
 *
 * Monitors the fiat cash buffer of a hybrid treasury portfolio against
 * projected outflows. When projected buffer falls below minBufferUSD,
 * signals the amount of T-bill liquidation needed to restore the
 * targetBufferUSD level.
 *
 * Runs on the fiat cash CSH contract.
 *
 * ACTUS contract type: CSH (cash position)
 * Market Object Codes consumed:
 *   PROJECTED_OUTFLOWS      — daily projected AP/operational outflows
 *   TBILL_MATURITY_SCHEDULE — nearest T-bill maturity value (for sizing)
 */
public class LiquidityBufferModel implements BehaviorRiskModelProvider {

    public static final String CALLOUT_TYPE = "MRD";

    private final String riskFactorId;
    private final String projectedOutflowsMOC;
    private final String tbillMaturityScheduleMOC;
    private final double minBufferUSD;
    private final double targetBufferUSD;
    private final List<String> monitoringEventTimes;
    private final MultiMarketRiskModel marketModel;

    public LiquidityBufferModel(String riskFactorId,
                                LiquidityBufferModelData data,
                                MultiMarketRiskModel marketModel) {
        this.riskFactorId             = riskFactorId;
        this.projectedOutflowsMOC     = data.getProjectedOutflowsMOC();
        this.tbillMaturityScheduleMOC = data.getTbillMaturityScheduleMOC();
        this.minBufferUSD             = data.getMinBufferUSD();
        this.targetBufferUSD          = data.getTargetBufferUSD();
        this.monitoringEventTimes     = data.getMonitoringEventTimes();
        this.marketModel              = marketModel;
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
                    System.out.println("**** LiquidityBufferModel: SKIPPING pre-IED callout " + eventTime + " (IED=" + ied + ")");
                    continue;
                }
            }
            callouts.add(new CalloutData(this.riskFactorId, eventTime, CALLOUT_TYPE));
        }
        return callouts;
    }

    @Override
    public double stateAt(String id, LocalDateTime time, StateSpace states) {

        double projectedOutflows = this.marketModel.stateAt(this.projectedOutflowsMOC, time);
        double tbillAvailable    = this.marketModel.stateAt(this.tbillMaturityScheduleMOC, time);

        double cashBalance = Math.abs(states.notionalPrincipal);
        double projectedBuffer = cashBalance - projectedOutflows;

        System.out.println("**** LiquidityBufferModel: time=" + time
                + " cashBalance=" + String.format("%.2f", cashBalance)
                + " projectedOutflows=" + String.format("%.2f", projectedOutflows)
                + " projectedBuffer=" + String.format("%.2f", projectedBuffer)
                + " minBuffer=" + String.format("%.2f", minBufferUSD)
                + " targetBuffer=" + String.format("%.2f", targetBufferUSD)
                + " tbillAvailable=" + String.format("%.2f", tbillAvailable));

        if (projectedBuffer >= this.minBufferUSD) {
            return 0.0;
        } else {
            double shortfall = this.minBufferUSD - projectedBuffer;
            double shortfallFraction = shortfall / this.targetBufferUSD;

            System.out.println("**** LiquidityBufferModel: SHORTFALL=$"
                    + String.format("%.2f", shortfall)
                    + " fraction=" + String.format("%.4f", shortfallFraction)
                    + " → liquidate T-bills (available=$"
                    + String.format("%.2f", tbillAvailable) + ")");
            return Math.min(1.0, shortfallFraction);
        }
    }
}
