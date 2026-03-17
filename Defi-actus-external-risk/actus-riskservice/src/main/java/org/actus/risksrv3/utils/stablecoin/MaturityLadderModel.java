package org.actus.risksrv3.utils.stablecoin;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.stablecoin.MaturityLadderModelData;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;
import org.actus.risksrv3.utils.MultiMarketRiskModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * MaturityLadderModel (Domain 5 — Model 6.3)
 *
 * Decides how to handle T-bill maturity events based on current de-peg
 * risk score. At each T-bill maturity (MRD event), evaluates:
 *
 *   PEG_RISK_SCORE < 15  → normal roll (return 0.0, T-bill renews)
 *   15 <= score < 30     → shorten tenor (return 0.25 signal)
 *   30 <= score < 45     → cash retention (return 0.50, keep as cash)
 *   score >= 45          → emergency liquidation (return 1.0, all to cash)
 *
 * Creates a self-adapting reserve portfolio that shifts from yield-seeking
 * to cash-defensive as de-peg risk increases.
 *
 * Market Object Code consumed:
 *   PEG_RISK_SCORE — composite risk score (0–100) from external assessment
 */
public class MaturityLadderModel implements BehaviorRiskModelProvider {

    public static final String CALLOUT_TYPE = "MRD";

    // Decision thresholds
    private static final double THRESHOLD_NORMAL   = 15.0;
    private static final double THRESHOLD_SHORTEN  = 30.0;
    private static final double THRESHOLD_CASH     = 45.0;

    // Return signals
    private static final double SIGNAL_NORMAL_ROLL        = 0.0;
    private static final double SIGNAL_SHORTEN_TENOR      = 0.25;
    private static final double SIGNAL_CASH_RETENTION     = 0.50;
    private static final double SIGNAL_EMERGENCY_LIQUIDATE = 1.0;

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final String riskFactorId;
    private final String pegRiskScoreMOC;
    private final List<String> monitoringEventTimes;
    private final MultiMarketRiskModel marketModel;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public MaturityLadderModel(String riskFactorId,
                               MaturityLadderModelData data,
                               MultiMarketRiskModel marketModel) {
        this.riskFactorId      = riskFactorId;
        this.pegRiskScoreMOC   = data.getPegRiskScoreMOC();
        this.monitoringEventTimes = data.getMonitoringEventTimes();
        this.marketModel       = marketModel;
    }

    // -------------------------------------------------------------------------
    // BehaviorRiskModelProvider interface
    // -------------------------------------------------------------------------

    @Override
    public Set<String> keys() {
        Set<String> keys = new HashSet<>();
        keys.add(this.riskFactorId);
        return keys;
    }

    @Override
    public List<CalloutData> contractStart(ContractModel contract) {
        // Contract-lifecycle scoped callouts: filter to [IED, maturityDate] window
        LocalDateTime ied = contract.getAs("initialExchangeDate");
        LocalDateTime maturity = contract.getAs("maturityDate");
        List<CalloutData> callouts = new ArrayList<>();
        for (String eventTime : this.monitoringEventTimes) {
            LocalDateTime eventDateTime = LocalDateTime.parse(eventTime);
            if (ied != null && eventDateTime.isBefore(ied)) {
                System.out.println("**** MaturityLadderModel: SKIPPING pre-IED callout " + eventTime + " (IED=" + ied + ")");
                continue;
            }
            if (maturity != null && eventDateTime.isAfter(maturity)) {
                System.out.println("**** MaturityLadderModel: SKIPPING post-maturity callout " + eventTime + " (maturity=" + maturity + ")");
                continue;
            }
            callouts.add(new CalloutData(this.riskFactorId, eventTime, CALLOUT_TYPE));
        }
        return callouts;
    }

    /**
     * Maturity decision tree based on peg risk score.
     *
     * @param id     Risk factor ID
     * @param time   Current evaluation time (at T-bill maturity)
     * @param states Current contract state
     * @return       0.0 (roll), 0.25 (shorten), 0.50 (cash), 1.0 (liquidate)
     */
    @Override
    public double stateAt(String id, LocalDateTime time, StateSpace states) {

        double pegRiskScore = this.marketModel.stateAt(this.pegRiskScoreMOC, time);

        String decision;
        double signal;

        if (pegRiskScore < THRESHOLD_NORMAL) {
            decision = "NORMAL_ROLL";
            signal = SIGNAL_NORMAL_ROLL;
        } else if (pegRiskScore < THRESHOLD_SHORTEN) {
            decision = "SHORTEN_TENOR";
            signal = SIGNAL_SHORTEN_TENOR;
        } else if (pegRiskScore < THRESHOLD_CASH) {
            decision = "CASH_RETENTION";
            signal = SIGNAL_CASH_RETENTION;
        } else {
            decision = "EMERGENCY_LIQUIDATION";
            signal = SIGNAL_EMERGENCY_LIQUIDATE;
        }

        System.out.println("**** MaturityLadderModel: time=" + time
                + " pegRiskScore=" + String.format("%.1f", pegRiskScore)
                + " decision=" + decision
                + " signal=" + signal
                + " notionalPrincipal=" + String.format("%.0f", states.notionalPrincipal));

        return signal;
    }
}
