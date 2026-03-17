package org.actus.risksrv3.utils.stablecoin;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.stablecoin.ComplianceDriftModelData;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;
import org.actus.risksrv3.utils.MultiMarketRiskModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ComplianceDriftModel (Domain 5 — Model 6.6)
 *
 * Continuous monitoring of regulatory compliance metrics against
 * STABLE Act, GENIUS Act, and MiCA EMT requirements.
 *
 * Monitors 4 compliance metrics with configurable weights:
 *
 *   Metric            | Weight | Threshold                | Source
 *   ─────────────────────────────────────────────────────────────────
 *   Backing ratio     | 40%    | >= 1.0 (100%)            | STABLE/GENIUS
 *   WAM (days)        | 30%    | <= 93 days               | GENIUS Act
 *   HQLA quality      | 20%    | >= 100 (all L1)          | GENIUS Act
 *   Attestation age   | 10%    | <= 30 days since last    | All frameworks
 *
 * Output: 365-day regulatory compliance timeline.
 * Returns: weighted breach score (0.0 = fully compliant, 1.0 = all breached)
 *
 * Market Object Codes consumed:
 *   SC_BACKING_RATIO      — current backing ratio
 *   SC_WAM_DAYS           — weighted average maturity in days
 *   SC_HQLA_SCORE         — HQLA quality score (0–100)
 *   SC_ATTESTATION_AGE    — days since last attestation
 */
public class ComplianceDriftModel implements BehaviorRiskModelProvider {

    public static final String CALLOUT_TYPE = "MRD";

    // Default compliance weights
    private static final double WEIGHT_BACKING     = 0.40;
    private static final double WEIGHT_WAM         = 0.30;
    private static final double WEIGHT_HQLA        = 0.20;
    private static final double WEIGHT_ATTESTATION = 0.10;

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final String riskFactorId;
    private final String backingRatioMOC;
    private final String wamDaysMOC;
    private final String hqlaScoreMOC;
    private final String attestationAgeMOC;
    private final double backingThreshold;        // 1.0 (100%)
    private final double wamMaxDays;              // 93.0 (GENIUS Act)
    private final double hqlaMinScore;            // 100.0 (all L1)
    private final double attestationMaxDays;      // 30.0
    private final List<String> monitoringEventTimes;
    private final MultiMarketRiskModel marketModel;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public ComplianceDriftModel(String riskFactorId,
                                ComplianceDriftModelData data,
                                MultiMarketRiskModel marketModel) {
        this.riskFactorId       = riskFactorId;
        this.backingRatioMOC    = data.getBackingRatioMOC();
        this.wamDaysMOC         = data.getWamDaysMOC();
        this.hqlaScoreMOC       = data.getHqlaScoreMOC();
        this.attestationAgeMOC  = data.getAttestationAgeMOC();
        this.backingThreshold   = data.getBackingThreshold();
        this.wamMaxDays         = data.getWamMaxDays();
        this.hqlaMinScore       = data.getHqlaMinScore();
        this.attestationMaxDays = data.getAttestationMaxDays();
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
        LocalDateTime ied = contract.getAs("initialExchangeDate");
        List<CalloutData> callouts = new ArrayList<>();
        for (String eventTime : this.monitoringEventTimes) {
            if (ied != null) {
                LocalDateTime eventDateTime = LocalDateTime.parse(eventTime);
                if (eventDateTime.isBefore(ied)) {
                    System.out.println("**** ComplianceDriftModel: SKIPPING pre-IED callout " + eventTime + " (IED=" + ied + ")");
                    continue;
                }
            }
            callouts.add(new CalloutData(this.riskFactorId, eventTime, CALLOUT_TYPE));
        }
        return callouts;
    }

    /**
     * Compliance drift logic — weighted breach score across 4 metrics.
     *
     * @return weighted breach score (0.0 = fully compliant, up to 1.0)
     */
    @Override
    public double stateAt(String id, LocalDateTime time, StateSpace states) {

        double backingRatio   = this.marketModel.stateAt(this.backingRatioMOC, time);
        double wamDays        = this.marketModel.stateAt(this.wamDaysMOC, time);
        double hqlaScore      = this.marketModel.stateAt(this.hqlaScoreMOC, time);
        double attestationAge = this.marketModel.stateAt(this.attestationAgeMOC, time);

        // Compute individual breach scores (0.0 = compliant, 1.0 = maximally breached)
        double backingBreach = (backingRatio >= this.backingThreshold) ? 0.0
                : Math.min(1.0, (this.backingThreshold - backingRatio) / this.backingThreshold);

        double wamBreach = (wamDays <= this.wamMaxDays) ? 0.0
                : Math.min(1.0, (wamDays - this.wamMaxDays) / this.wamMaxDays);

        double hqlaBreach = (hqlaScore >= this.hqlaMinScore) ? 0.0
                : Math.min(1.0, (this.hqlaMinScore - hqlaScore) / this.hqlaMinScore);

        double attestationBreach = (attestationAge <= this.attestationMaxDays) ? 0.0
                : Math.min(1.0, (attestationAge - this.attestationMaxDays) / this.attestationMaxDays);

        // Weighted composite score
        double compositeScore = WEIGHT_BACKING * backingBreach
                + WEIGHT_WAM * wamBreach
                + WEIGHT_HQLA * hqlaBreach
                + WEIGHT_ATTESTATION * attestationBreach;

        System.out.println("**** ComplianceDriftModel: time=" + time
                + " backing=" + String.format("%.4f", backingRatio)
                + "(breach=" + String.format("%.3f", backingBreach) + ")"
                + " WAM=" + String.format("%.0f", wamDays) + "d"
                + "(breach=" + String.format("%.3f", wamBreach) + ")"
                + " HQLA=" + String.format("%.0f", hqlaScore)
                + "(breach=" + String.format("%.3f", hqlaBreach) + ")"
                + " attestAge=" + String.format("%.0f", attestationAge) + "d"
                + "(breach=" + String.format("%.3f", attestationBreach) + ")"
                + " COMPOSITE=" + String.format("%.4f", compositeScore));

        return compositeScore;
    }
}
