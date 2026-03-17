package org.actus.risksrv3.utils.stablecoin;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.stablecoin.ConcentrationDriftModelData;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;
import org.actus.risksrv3.utils.MultiMarketRiskModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ConcentrationDriftModel (Domain 5 — Model 6.5)
 *
 * Computes Herfindahl-Hirschman Index (HHI) across reserve asset buckets
 * and monitors single-asset concentration. Concentration changes dynamically
 * as T-bills mature, redemptions deplete cash, and compositions shift.
 *
 * HHI = Sum(share_i²) across all asset buckets.
 *   HHI = 1.0 → perfectly concentrated (single asset)
 *   HHI = 1/N → perfectly diversified across N equal assets
 *
 * Signals rebalancing when single-asset concentration > maxSingleAssetShare (40%).
 *
 * Historical finding: "other" ratio of 57% was key differentiator between
 * stablecoins that survived vs failed de-peg events.
 *
 * CUSTODIAN CONCENTRATION (SVB lesson):
 *   If custodianBucketMOCs is provided, also tracks custodian-level concentration.
 *   Prevents SVB scenario: $3.3B cash "frozen" due to single-bank concentration.
 *   Custodian HHI checked independently with same thresholds.
 *
 * Market Object Codes consumed (asset buckets):
 *   SC_BUCKET_CASH     — cash holdings value
 *   SC_BUCKET_4W_TBILL — 4-week T-bill value
 *   SC_BUCKET_13W_TBILL — 13-week T-bill value
 *   SC_BUCKET_26W_TBILL — 26-week T-bill value
 *
 * Optional custodian buckets:
 *   SC_CUSTODIAN_BOFA  — Bank of America holdings
 *   SC_CUSTODIAN_JPM   — JPMorgan holdings
 *   SC_CUSTODIAN_CITI  — Citibank holdings
 *   SC_CUSTODIAN_WF    — Wells Fargo holdings
 *
 * Returns: excess concentration fraction above threshold (0.0 if diversified)
 */
public class ConcentrationDriftModel implements BehaviorRiskModelProvider {

    public static final String CALLOUT_TYPE = "MRD";

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final String riskFactorId;
    private final List<String> assetBucketMOCs;     // market object codes for each asset bucket
    private final List<String> custodianBucketMOCs; // optional custodian buckets
    private final double maxSingleAssetShare;       // 0.40 (40% maximum)
    private final double hhiWarningThreshold;       // 0.35 (HHI above this = warning)
    private final List<String> monitoringEventTimes;
    private final MultiMarketRiskModel marketModel;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public ConcentrationDriftModel(String riskFactorId,
                                   ConcentrationDriftModelData data,
                                   MultiMarketRiskModel marketModel) {
        this.riskFactorId         = riskFactorId;
        this.assetBucketMOCs      = data.getAssetBucketMOCs();
        this.custodianBucketMOCs  = data.getCustodianBucketMOCs(); // BACKWARD COMPATIBLE - can be null
        this.maxSingleAssetShare  = data.getMaxSingleAssetShare();
        this.hhiWarningThreshold  = data.getHhiWarningThreshold();
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
                    System.out.println("**** ConcentrationDriftModel: SKIPPING pre-IED callout " + eventTime + " (IED=" + ied + ")");
                    continue;
                }
            }
            callouts.add(new CalloutData(this.riskFactorId, eventTime, CALLOUT_TYPE));
        }
        return callouts;
    }

    /**
     * Concentration drift logic — computes asset HHI and optional custodian HHI.
     *
     * @return excess concentration fraction (0.0 if within limits, up to 1.0)
     */
    @Override
    public double stateAt(String id, LocalDateTime time, StateSpace states) {

        // ===== ASSET TYPE CONCENTRATION =====
        double totalValue = 0.0;
        double[] assetBucketValues = new double[this.assetBucketMOCs.size()];

        for (int i = 0; i < this.assetBucketMOCs.size(); i++) {
            assetBucketValues[i] = this.marketModel.stateAt(this.assetBucketMOCs.get(i), time);
            totalValue += assetBucketValues[i];
        }

        // Guard against zero total
        if (totalValue <= 0.0) {
            System.out.println("**** ConcentrationDriftModel: time=" + time
                    + " WARNING: totalValue <= 0, returning 0.0");
            return 0.0;
        }

        // Compute asset shares and HHI
        double assetHHI = 0.0;
        double maxAssetShare = 0.0;
        String maxAssetBucket = "";

        for (int i = 0; i < assetBucketValues.length; i++) {
            double share = assetBucketValues[i] / totalValue;
            assetHHI += share * share;
            if (share > maxAssetShare) {
                maxAssetShare = share;
                maxAssetBucket = this.assetBucketMOCs.get(i);
            }
        }

        System.out.println("**** ConcentrationDriftModel [ASSET]: time=" + time
                + " totalValue=" + String.format("%.0f", totalValue)
                + " HHI=" + String.format("%.4f", assetHHI)
                + " maxShare=" + String.format("%.4f", maxAssetShare)
                + " maxBucket=" + maxAssetBucket);

        // ===== CUSTODIAN CONCENTRATION (OPTIONAL) =====
        double custodianHHI = 0.0;
        double maxCustodianShare = 0.0;
        String maxCustodianBucket = "";

        if (this.custodianBucketMOCs != null && !this.custodianBucketMOCs.isEmpty()) {
            double custodianTotal = 0.0;
            double[] custodianBucketValues = new double[this.custodianBucketMOCs.size()];

            for (int i = 0; i < this.custodianBucketMOCs.size(); i++) {
                custodianBucketValues[i] = this.marketModel.stateAt(this.custodianBucketMOCs.get(i), time);
                custodianTotal += custodianBucketValues[i];
            }

            if (custodianTotal > 0.0) {
                for (int i = 0; i < custodianBucketValues.length; i++) {
                    double share = custodianBucketValues[i] / custodianTotal;
                    custodianHHI += share * share;
                    if (share > maxCustodianShare) {
                        maxCustodianShare = share;
                        maxCustodianBucket = this.custodianBucketMOCs.get(i);
                    }
                }

                System.out.println("**** ConcentrationDriftModel [CUSTODIAN]: time=" + time
                        + " totalValue=" + String.format("%.0f", custodianTotal)
                        + " HHI=" + String.format("%.4f", custodianHHI)
                        + " maxShare=" + String.format("%.4f", maxCustodianShare)
                        + " maxBucket=" + maxCustodianBucket);
            }
        }

        // ===== CHECK VIOLATIONS =====
        double concentrationRisk = 0.0;

        // Check asset single-asset concentration breach
        if (maxAssetShare > this.maxSingleAssetShare) {
            double excessConcentration = maxAssetShare - this.maxSingleAssetShare;
            System.out.println("**** ConcentrationDriftModel: ASSET_CONCENTRATION_BREACH"
                    + " excess=" + String.format("%.4f", excessConcentration));
            concentrationRisk = Math.max(concentrationRisk, excessConcentration);
        }

        // Check asset HHI warning
        if (assetHHI > this.hhiWarningThreshold) {
            double hhiExcess = (assetHHI - this.hhiWarningThreshold) * 0.5; // scaled signal
            System.out.println("**** ConcentrationDriftModel: ASSET_HHI_WARNING"
                    + " hhiExcess=" + String.format("%.4f", hhiExcess));
            concentrationRisk = Math.max(concentrationRisk, hhiExcess);
        }

        // Check custodian concentration if configured
        if (this.custodianBucketMOCs != null && !this.custodianBucketMOCs.isEmpty()) {
            if (maxCustodianShare > this.maxSingleAssetShare) {
                double excessConcentration = maxCustodianShare - this.maxSingleAssetShare;
                System.out.println("**** ConcentrationDriftModel: CUSTODIAN_CONCENTRATION_BREACH"
                        + " excess=" + String.format("%.4f", excessConcentration));
                concentrationRisk = Math.max(concentrationRisk, excessConcentration);
            }

            if (custodianHHI > this.hhiWarningThreshold) {
                double hhiExcess = (custodianHHI - this.hhiWarningThreshold) * 0.5; // scaled signal
                System.out.println("**** ConcentrationDriftModel: CUSTODIAN_HHI_WARNING"
                        + " hhiExcess=" + String.format("%.4f", hhiExcess));
                concentrationRisk = Math.max(concentrationRisk, hhiExcess);
            }
        }

        return Math.min(1.0, concentrationRisk);
    }
}
