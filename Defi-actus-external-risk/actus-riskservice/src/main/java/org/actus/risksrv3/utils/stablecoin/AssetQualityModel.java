package org.actus.risksrv3.utils.stablecoin;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.stablecoin.AssetQualityModelData;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;
import org.actus.risksrv3.utils.MultiMarketRiskModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * AssetQualityModel (Domain 5 — Model 6.4)
 *
 * Monitors external stress indices to dynamically degrade HQLA quality
 * scores for reserve assets. HQLA categorization is normally static
 * (L1=100, L2A=85, L2B=50) but effective quality changes under stress —
 * e.g. SVB/USDC March 2023: $3.3B technically "cash" but frozen.
 *
 * Monitors two reference indices:
 *   BANK_STRESS_INDEX     — custodian bank health (0.0=healthy, 1.0=failing)
 *   US_SOVEREIGN_STRESS   — sovereign credit stress (0.0=normal, 1.0=extreme)
 *
 * Quality degradation logic:
 *   Bank stress >= bankStressThreshold (0.5):
 *     effectiveQuality = baseQuality × (1 - bankStress)
 *     e.g. stress=1.0 → quality drops from 100 to 0 (SVB frozen)
 *     e.g. stress=0.5 → quality drops from 100 to 50
 *
 *   Sovereign stress:
 *     Maximum 30% degradation on T-bill quality
 *     effectiveQuality = baseQuality × (1 - 0.3 × sovereignStress)
 *
 *   Floor at qualityFloor (50) — L2B minimum classification.
 *
 * STRICT MODE (GENIUS Act §4(a)(1)):
 *   If strictMode=true and hqlaMOC is provided, reads published HQLA score.
 *   If HQLA < 100, returns maximum degradation (1.0) to trigger violation.
 *   This enforces "all L1 assets" requirement with zero tolerance.
 *
 * Returns: quality degradation fraction (0.0 = no degradation, up to 1.0)
 */
public class AssetQualityModel implements BehaviorRiskModelProvider {

    public static final String CALLOUT_TYPE = "MRD";

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final String riskFactorId;
    private final String bankStressIndexMOC;
    private final String sovereignStressMOC;
    private final double bankStressThreshold;    // 0.5
    private final double baseQuality;            // 100.0 (L1 HQLA)
    private final double qualityFloor;           // 50.0  (L2B minimum)
    private final double sovereignMaxDegradation; // 0.30 (30% max)
    private final boolean strictMode;            // GENIUS Act compliance
    private final String hqlaMOC;                // Optional published HQLA index
    private final List<String> monitoringEventTimes;
    private final MultiMarketRiskModel marketModel;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public AssetQualityModel(String riskFactorId,
                             AssetQualityModelData data,
                             MultiMarketRiskModel marketModel) {
        this.riskFactorId           = riskFactorId;
        this.bankStressIndexMOC     = data.getBankStressIndexMOC();
        this.sovereignStressMOC     = data.getSovereignStressMOC();
        this.bankStressThreshold    = data.getBankStressThreshold();
        this.baseQuality            = data.getBaseQuality();
        this.qualityFloor           = data.getQualityFloor();
        this.sovereignMaxDegradation = data.getSovereignMaxDegradation();
        this.strictMode             = data.isStrictMode();  // BACKWARD COMPATIBLE - defaults to false
        this.hqlaMOC                = data.getHqlaMOC();    // BACKWARD COMPATIBLE - can be null
        this.monitoringEventTimes   = data.getMonitoringEventTimes();
        this.marketModel            = marketModel;
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
        // PP-before-IED fix: filter out callouts before contract starts
        LocalDateTime ied = contract.getAs("initialExchangeDate");
        List<CalloutData> callouts = new ArrayList<>();
        for (String eventTime : this.monitoringEventTimes) {
            if (ied != null) {
                LocalDateTime eventDateTime = LocalDateTime.parse(eventTime);
                if (eventDateTime.isBefore(ied)) {
                    System.out.println("**** AssetQualityModel: SKIPPING pre-IED callout " + eventTime + " (IED=" + ied + ")");
                    continue;
                }
            }
            callouts.add(new CalloutData(this.riskFactorId, eventTime, CALLOUT_TYPE));
        }
        return callouts;
    }

    /**
     * Asset quality degradation logic with optional strict HQLA enforcement.
     *
     * @return quality degradation fraction (0.0 = no degradation, up to 1.0)
     */
    @Override
    public double stateAt(String id, LocalDateTime time, StateSpace states) {

        // STRICT MODE: Check published HQLA score if configured (GENIUS Act §4(a)(1))
        if (this.strictMode && this.hqlaMOC != null && !this.hqlaMOC.isEmpty()) {
            double publishedHQLA = this.marketModel.stateAt(this.hqlaMOC, time);
            if (publishedHQLA < 100.0) {
                System.out.println("**** AssetQualityModel [STRICT]: time=" + time
                        + " HQLA=" + String.format("%.1f", publishedHQLA)
                        + " VIOLATION: GENIUS Act requires 100% L1 assets"
                        + " degradationFraction=1.0 (MAX)");
                return 1.0; // Maximum degradation - triggers violation
            }
        }

        // Standard stress-based degradation logic
        double bankStress = this.marketModel.stateAt(this.bankStressIndexMOC, time);
        double sovereignStress = this.marketModel.stateAt(this.sovereignStressMOC, time);

        double effectiveQuality = this.baseQuality;

        // Bank stress degradation (applies to cash deposits)
        if (bankStress >= this.bankStressThreshold) {
            double bankDegradation = bankStress; // 0.5 stress → 50% quality loss
            effectiveQuality = this.baseQuality * (1.0 - bankDegradation);
        }

        // Sovereign stress degradation (applies to T-bills)
        if (sovereignStress > 0.0) {
            double sovDegradation = this.sovereignMaxDegradation * sovereignStress;
            double sovAdjusted = this.baseQuality * (1.0 - sovDegradation);
            effectiveQuality = Math.min(effectiveQuality, sovAdjusted);
        }

        // Apply floor
        effectiveQuality = Math.max(effectiveQuality, this.qualityFloor);

        // Compute degradation fraction (0.0 = no degradation, 0.5 = 50% degraded)
        double degradationFraction = (this.baseQuality - effectiveQuality) / this.baseQuality;

        System.out.println("**** AssetQualityModel: time=" + time
                + " strictMode=" + this.strictMode
                + " bankStress=" + String.format("%.3f", bankStress)
                + " sovereignStress=" + String.format("%.3f", sovereignStress)
                + " effectiveQuality=" + String.format("%.1f", effectiveQuality)
                + " degradationFraction=" + String.format("%.4f", degradationFraction));

        return degradationFraction;
    }
}
