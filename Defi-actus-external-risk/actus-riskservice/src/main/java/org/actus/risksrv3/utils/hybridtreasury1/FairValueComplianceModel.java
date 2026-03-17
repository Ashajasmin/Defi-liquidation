package org.actus.risksrv3.utils.hybridtreasury1;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.hybridtreasury1.FairValueComplianceModelData;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;
import org.actus.risksrv3.utils.MultiMarketRiskModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * FairValueComplianceModel (Domain 4 — Treasury Model 5.7)
 *
 * Monitors FASB ASU 2023-08 fair value accounting for digital assets
 * and MSCI exclusion probability. Runs on each digital asset STK
 * contract at periodic monitoring points (typically quarterly).
 *
 * Computes fair value delta between current market value and book value.
 * Checks MSCI exclusion probability index — if probability exceeds
 * threshold, signals potential compliance action.
 *
 * Decision logic at each check date:
 *   currentPrice = ASSET_FAIR_VALUE from marketModel
 *   msciExclusionProb = MSCI_EXCLUSION_PROB from marketModel
 *   bookValue = states.notionalPrincipal (carrying value on books)
 *
 *   fairValueDelta = (currentPrice × quantity - bookValue) / bookValue
 *
 *   msciExclusionProb > msciThreshold → return msciExclusionProb
 *     (signal: ESG/compliance risk, consider position reduction)
 *   abs(fairValueDelta) > large threshold → return fairValueDelta
 *     (signal: material fair value change, mark-to-market required)
 *   else → return 0.0 (no compliance concern)
 *
 * Regulatory context:
 *   FASB ASU 2023-08: Crypto assets measured at fair value through
 *   income statement. Quarterly unrealized gains/losses impact P&L.
 *   MSCI ESG exclusion: 15% probability for BTC-heavy corporates.
 *
 * ACTUS contract type: STK (digital asset position)
 * Market Object Codes consumed:
 *   ASSET_FAIR_VALUE    — current fair market value per unit
 *   MSCI_EXCLUSION_PROB — MSCI ESG exclusion probability (0.0–1.0)
 *
 * Place this file in:
 *   src/main/java/org/actus/risksrv3/utils/hybridtreasury1/
 */
public class FairValueComplianceModel implements BehaviorRiskModelProvider {

    public static final String CALLOUT_TYPE = "MRD";

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final String riskFactorId;
    private final String assetFairValueMOC;      // e.g. BTC_FAIR_VALUE or ETH_FAIR_VALUE
    private final String msciExclusionProbMOC;   // e.g. MSCI_EXCLUSION_PROB
    private final double msciThreshold;          // e.g. 0.15 (15%)
    private final double materialityThreshold;   // e.g. 0.10 (10% fair value change)
    private final List<String> monitoringEventTimes;
    private final MultiMarketRiskModel marketModel;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public FairValueComplianceModel(String riskFactorId,
                                    FairValueComplianceModelData data,
                                    MultiMarketRiskModel marketModel) {
        this.riskFactorId         = riskFactorId;
        this.assetFairValueMOC    = data.getAssetFairValueMOC();
        this.msciExclusionProbMOC = data.getMsciExclusionProbMOC();
        this.msciThreshold        = data.getMsciThreshold();
        this.materialityThreshold = data.getMaterialityThreshold();
        this.monitoringEventTimes = data.getMonitoringEventTimes();
        this.marketModel          = marketModel;
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
        LocalDateTime ied = contract.getAs("initialExchangeDate");
        List<CalloutData> callouts = new ArrayList<>();
        for (String eventTime : this.monitoringEventTimes) {
            if (ied != null) { LocalDateTime edt = LocalDateTime.parse(eventTime); if (edt.isBefore(ied)) { System.out.println("**** FairValueComplianceModel: SKIPPING pre-IED callout " + eventTime + " (IED=" + ied + ")"); continue; } }
            callouts.add(new CalloutData(this.riskFactorId, eventTime, CALLOUT_TYPE));
        }
        return callouts;
    }

    /**
     * Fair value compliance logic — called at each monitoring point.
     *
     * Computes fair value delta and checks MSCI exclusion risk.
     * Returns the more severe of the two signals.
     *
     * @param id     Risk factor ID
     * @param time   Current evaluation time
     * @param states Current contract state (notionalPrincipal = book value)
     * @return       0.0 if compliant, positive fraction if action needed
     */
    @Override
    public double stateAt(String id, LocalDateTime time, StateSpace states) {

        double fairValue       = this.marketModel.stateAt(this.assetFairValueMOC, time);
        double msciExclusion   = this.marketModel.stateAt(this.msciExclusionProbMOC, time);

        double bookValue = Math.abs(states.notionalPrincipal);

        // Compute fair value delta
        double fairValueDelta = 0.0;
        if (bookValue > 0.0) {
            fairValueDelta = (fairValue - bookValue) / bookValue;
        }

        System.out.println("**** FairValueComplianceModel: time=" + time
                + " fairValue=" + String.format("%.2f", fairValue)
                + " bookValue=" + String.format("%.2f", bookValue)
                + " fairValueDelta=" + String.format("%.4f", fairValueDelta)
                + " msciExclusion=" + String.format("%.4f", msciExclusion)
                + " msciThreshold=" + msciThreshold);

        double signal = 0.0;

        // Check MSCI exclusion probability
        if (msciExclusion > this.msciThreshold) {
            System.out.println("**** FairValueComplianceModel: MSCI_EXCLUSION_RISK="
                    + String.format("%.4f", msciExclusion)
                    + " > " + msciThreshold
                    + " → ESG compliance action");
            signal = Math.max(signal, msciExclusion);
        }

        // Check materiality of fair value change
        if (Math.abs(fairValueDelta) > this.materialityThreshold) {
            System.out.println("**** FairValueComplianceModel: MATERIAL_FV_CHANGE="
                    + String.format("%.4f", fairValueDelta)
                    + " → FASB ASU 2023-08 mark-to-market impact");
            signal = Math.max(signal, Math.abs(fairValueDelta));
        }

        return Math.min(1.0, signal);
    }
}
