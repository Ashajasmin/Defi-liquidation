package org.actus.risksrv3.utils.stablecoin;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.stablecoin.ContinuousAttestationModelData;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;
import org.actus.risksrv3.utils.MultiMarketRiskModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ContinuousAttestationModel (Domain 5 — Model 6.8)
 *
 * Aggregates metrics from all other stablecoin models into a single
 * composite risk score for daily ZK attestation data packaging.
 *
 * Transforms ZK-PRET from point-in-time attestation to continuous
 * compliance verification across 365-day simulation horizon.
 *
 * Aggregation weights:
 *   Backing ratio risk    : 30%
 *   Liquidity risk        : 25%
 *   Asset quality risk    : 15%
 *   Concentration risk    : 10%
 *   Compliance drift risk : 15%
 *   Early warning risk    : 5%
 *
 * Composite score interpretation:
 *   0.0         → FULLY_COMPLIANT
 *   < 0.10      → MINOR_DEVIATION
 *   < 0.30      → WARNING
 *   < 0.60      → BREACH
 *   >= 0.60     → CRITICAL_BREACH
 *
 * Market Object Codes consumed:
 *   SC_BACKING_RISK       — backing ratio risk score (from BackingRatioModel)
 *   SC_LIQUIDITY_RISK     — liquidity risk score (from RedemptionPressureModel)
 *   SC_QUALITY_RISK       — asset quality risk (from AssetQualityModel)
 *   SC_CONCENTRATION_RISK — concentration risk (from ConcentrationDriftModel)
 *   SC_COMPLIANCE_RISK    — compliance drift risk (from ComplianceDriftModel)
 *   SC_EARLYWARNING_RISK  — early warning risk (from EarlyWarningModel)
 */
public class ContinuousAttestationModel implements BehaviorRiskModelProvider {

    public static final String CALLOUT_TYPE = "MRD";

    // Aggregation weights
    private static final double WEIGHT_BACKING       = 0.30;
    private static final double WEIGHT_LIQUIDITY     = 0.25;
    private static final double WEIGHT_QUALITY       = 0.15;
    private static final double WEIGHT_CONCENTRATION = 0.10;
    private static final double WEIGHT_COMPLIANCE    = 0.15;
    private static final double WEIGHT_EARLYWARNING  = 0.05;

    // Status thresholds
    private static final double THRESHOLD_MINOR   = 0.10;
    private static final double THRESHOLD_WARNING = 0.30;
    private static final double THRESHOLD_BREACH  = 0.60;

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final String riskFactorId;
    private final String backingRiskMOC;
    private final String liquidityRiskMOC;
    private final String qualityRiskMOC;
    private final String concentrationRiskMOC;
    private final String complianceRiskMOC;
    private final String earlyWarningRiskMOC;
    private final List<String> monitoringEventTimes;
    private final MultiMarketRiskModel marketModel;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public ContinuousAttestationModel(String riskFactorId,
                                      ContinuousAttestationModelData data,
                                      MultiMarketRiskModel marketModel) {
        this.riskFactorId         = riskFactorId;
        this.backingRiskMOC       = data.getBackingRiskMOC();
        this.liquidityRiskMOC     = data.getLiquidityRiskMOC();
        this.qualityRiskMOC       = data.getQualityRiskMOC();
        this.concentrationRiskMOC = data.getConcentrationRiskMOC();
        this.complianceRiskMOC    = data.getComplianceRiskMOC();
        this.earlyWarningRiskMOC  = data.getEarlyWarningRiskMOC();
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
            if (ied != null) {
                LocalDateTime eventDateTime = LocalDateTime.parse(eventTime);
                if (eventDateTime.isBefore(ied)) {
                    System.out.println("**** ContinuousAttestationModel: SKIPPING pre-IED callout " + eventTime + " (IED=" + ied + ")");
                    continue;
                }
            }
            callouts.add(new CalloutData(this.riskFactorId, eventTime, CALLOUT_TYPE));
        }
        return callouts;
    }

    /**
     * Continuous attestation aggregation — weighted composite of all metrics.
     *
     * @return composite risk score (0.0 = fully compliant, up to 1.0)
     */
    @Override
    public double stateAt(String id, LocalDateTime time, StateSpace states) {

        double backingRisk       = this.marketModel.stateAt(this.backingRiskMOC, time);
        double liquidityRisk     = this.marketModel.stateAt(this.liquidityRiskMOC, time);
        double qualityRisk       = this.marketModel.stateAt(this.qualityRiskMOC, time);
        double concentrationRisk = this.marketModel.stateAt(this.concentrationRiskMOC, time);
        double complianceRisk    = this.marketModel.stateAt(this.complianceRiskMOC, time);
        double earlyWarningRisk  = this.marketModel.stateAt(this.earlyWarningRiskMOC, time);

        // Weighted composite
        double compositeScore = WEIGHT_BACKING * backingRisk
                + WEIGHT_LIQUIDITY * liquidityRisk
                + WEIGHT_QUALITY * qualityRisk
                + WEIGHT_CONCENTRATION * concentrationRisk
                + WEIGHT_COMPLIANCE * complianceRisk
                + WEIGHT_EARLYWARNING * earlyWarningRisk;

        compositeScore = Math.min(1.0, compositeScore);

        // Determine status
        String status;
        if (compositeScore == 0.0) {
            status = "FULLY_COMPLIANT";
        } else if (compositeScore < THRESHOLD_MINOR) {
            status = "MINOR_DEVIATION";
        } else if (compositeScore < THRESHOLD_WARNING) {
            status = "WARNING";
        } else if (compositeScore < THRESHOLD_BREACH) {
            status = "BREACH";
        } else {
            status = "CRITICAL_BREACH";
        }

        System.out.println("**** ContinuousAttestationModel: time=" + time
                + " backing=" + String.format("%.3f", backingRisk)
                + " liquidity=" + String.format("%.3f", liquidityRisk)
                + " quality=" + String.format("%.3f", qualityRisk)
                + " concentration=" + String.format("%.3f", concentrationRisk)
                + " compliance=" + String.format("%.3f", complianceRisk)
                + " earlyWarning=" + String.format("%.3f", earlyWarningRisk)
                + " COMPOSITE=" + String.format("%.4f", compositeScore)
                + " STATUS=" + status);

        return compositeScore;
    }
}
