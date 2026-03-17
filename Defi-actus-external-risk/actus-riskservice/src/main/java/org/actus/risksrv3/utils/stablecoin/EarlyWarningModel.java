package org.actus.risksrv3.utils.stablecoin;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.stablecoin.EarlyWarningModelData;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;
import org.actus.risksrv3.utils.MultiMarketRiskModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * EarlyWarningModel (Domain 5 — Model 6.7)
 *
 * Monitors 4 validated early warning indicators and determines
 * defensive posture for the reserve portfolio.
 *
 * Validated signals (from 609-event research):
 *
 *   Signal                        | Threshold | Lead Time | F1 Score
 *   ─────────────────────────────────────────────────────────────────
 *   Curve pool imbalance          | > 15%     | 4.2 hrs   | 0.80
 *   Order book depth decline      | > 40%     | 2.1 hrs   | 0.70
 *   CEX outflows spike            | > 3x      | 6.8 hrs   | 0.69
 *   Social sentiment drop         | > 2 std   | 8.4 hrs   | 0.63
 *
 * Defensive posture levels:
 *   0 active signals → NORMAL         (return 0.0)
 *   1 active signal  → WATCH          (return 0.1)
 *   2 active signals → ELEVATED       (return 0.3)
 *   3 active signals → HIGH_ALERT     (return 0.6)
 *   4 active signals → MAX_DEFENSIVE  (return 1.0)
 *
 * When 2+ signals fire simultaneously, triggers pre-emptive response
 * BEFORE de-peg hits: accumulate cash to >35% (94% survival threshold).
 *
 * Market Object Codes consumed:
 *   SC_CURVE_IMBALANCE    — curve pool imbalance fraction
 *   SC_ORDERBOOK_DECLINE  — order book depth decline fraction
 *   SC_CEX_OUTFLOW_MULT   — CEX outflow multiplier vs baseline
 *   SC_SENTIMENT_ZSCORE   — social sentiment z-score (negative = bearish)
 */
public class EarlyWarningModel implements BehaviorRiskModelProvider {

    public static final String CALLOUT_TYPE = "MRD";

    // Signal thresholds (from validated research)
    private static final double CURVE_IMBALANCE_THRESHOLD  = 0.15;  // 15%
    private static final double ORDERBOOK_DECLINE_THRESHOLD = 0.40;  // 40%
    private static final double CEX_OUTFLOW_THRESHOLD       = 3.0;   // 3x baseline
    private static final double SENTIMENT_ZSCORE_THRESHOLD  = -2.0;  // 2 std below

    // Posture return signals
    private static final double SIGNAL_NORMAL        = 0.0;
    private static final double SIGNAL_WATCH         = 0.1;
    private static final double SIGNAL_ELEVATED      = 0.3;
    private static final double SIGNAL_HIGH_ALERT    = 0.6;
    private static final double SIGNAL_MAX_DEFENSIVE = 1.0;

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final String riskFactorId;
    private final String curveImbalanceMOC;
    private final String orderbookDeclineMOC;
    private final String cexOutflowMOC;
    private final String sentimentZscoreMOC;
    private final List<String> monitoringEventTimes;
    private final MultiMarketRiskModel marketModel;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public EarlyWarningModel(String riskFactorId,
                             EarlyWarningModelData data,
                             MultiMarketRiskModel marketModel) {
        this.riskFactorId       = riskFactorId;
        this.curveImbalanceMOC  = data.getCurveImbalanceMOC();
        this.orderbookDeclineMOC = data.getOrderbookDeclineMOC();
        this.cexOutflowMOC      = data.getCexOutflowMOC();
        this.sentimentZscoreMOC = data.getSentimentZscoreMOC();
        this.monitoringEventTimes = data.getMonitoringEventTimes();
        this.marketModel        = marketModel;
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
                System.out.println("**** EarlyWarningModel: SKIPPING pre-IED callout " + eventTime + " (IED=" + ied + ")");
                continue;
            }
            if (maturity != null && eventDateTime.isAfter(maturity)) {
                System.out.println("**** EarlyWarningModel: SKIPPING post-maturity callout " + eventTime + " (maturity=" + maturity + ")");
                continue;
            }
            callouts.add(new CalloutData(this.riskFactorId, eventTime, CALLOUT_TYPE));
        }
        return callouts;
    }

    /**
     * Early warning logic — counts active signals, returns posture level.
     *
     * @return posture signal (0.0=normal to 1.0=max defensive)
     */
    @Override
    public double stateAt(String id, LocalDateTime time, StateSpace states) {

        double curveImbalance  = this.marketModel.stateAt(this.curveImbalanceMOC, time);
        double orderbookDecline = this.marketModel.stateAt(this.orderbookDeclineMOC, time);
        double cexOutflowMult  = this.marketModel.stateAt(this.cexOutflowMOC, time);
        double sentimentZscore = this.marketModel.stateAt(this.sentimentZscoreMOC, time);

        // Count active signals
        int activeSignals = 0;
        StringBuilder signalLog = new StringBuilder();

        if (curveImbalance > CURVE_IMBALANCE_THRESHOLD) {
            activeSignals++;
            signalLog.append("CURVE_IMBALANCE(").append(String.format("%.3f", curveImbalance)).append(") ");
        }
        if (orderbookDecline > ORDERBOOK_DECLINE_THRESHOLD) {
            activeSignals++;
            signalLog.append("ORDERBOOK_DECLINE(").append(String.format("%.3f", orderbookDecline)).append(") ");
        }
        if (cexOutflowMult > CEX_OUTFLOW_THRESHOLD) {
            activeSignals++;
            signalLog.append("CEX_OUTFLOW(").append(String.format("%.1f", cexOutflowMult)).append("x) ");
        }
        if (sentimentZscore < SENTIMENT_ZSCORE_THRESHOLD) {
            activeSignals++;
            signalLog.append("SENTIMENT(z=").append(String.format("%.2f", sentimentZscore)).append(") ");
        }

        // Map signal count to posture
        double posture;
        String postureName;
        switch (activeSignals) {
            case 0:  posture = SIGNAL_NORMAL;        postureName = "NORMAL";        break;
            case 1:  posture = SIGNAL_WATCH;         postureName = "WATCH";         break;
            case 2:  posture = SIGNAL_ELEVATED;      postureName = "ELEVATED";      break;
            case 3:  posture = SIGNAL_HIGH_ALERT;    postureName = "HIGH_ALERT";    break;
            default: posture = SIGNAL_MAX_DEFENSIVE; postureName = "MAX_DEFENSIVE"; break;
        }

        System.out.println("**** EarlyWarningModel: time=" + time
                + " activeSignals=" + activeSignals
                + " posture=" + postureName + "(" + posture + ")"
                + " signals=[" + signalLog.toString().trim() + "]");

        return posture;
    }
}
