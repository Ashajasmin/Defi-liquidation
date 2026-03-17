package org.actus.risksrv3.utils.defiliquidation1;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.defiliquidation1.CollateralVelocityModelData;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;
import org.actus.risksrv3.utils.MultiMarketRiskModel;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * CollateralVelocityModel (Domain 1 — Model 2.3)
 *
 * Monitors the RATE OF CHANGE of LTV deterioration, not just LTV level.
 * Provides early warning by computing dLTV/dt (velocity) and estimating
 * days-to-liquidation.
 *
 * Key insight: ETH dropping $50/day vs $500/day both show "LTV = 75%" but
 * have very different urgency. This model triggers pre-emptive intervention
 * when velocity indicates imminent liquidation even if current LTV is safe.
 *
 * Decision logic:
 *   daysToLiquidation > safeHorizonDays  → return 0.0 (healthy pace)
 *   daysToLiquidation <= urgentDays       → return fraction for aggressive repay
 *   daysToLiquidation <= safeHorizonDays  → return smaller fraction for moderate repay
 *
 * Maintains rolling window of LTV values from previous callouts.
 *
 * Volatility data sources for velocity estimation:
 *   - CoinGecko API: hourly/daily OHLCV for ETH price velocity
 *   - Deribit ETH DVOL: implied volatility index (behavioral forward-looking)
 *   - CryptoDataDownload API: historical DVOL OHLC values
 *   - Glassnode: realized volatility, SOPR, exchange net flows
 *   - ETH ETF flow data (farside.co.uk/eth): institutional behavioral signal
 *     Large outflows → bearish velocity signal → tighten velocity thresholds
 *
 * Historical ETH volatility context for calibration:
 *   2017-2018: Annualized vol 80-150% (ICO boom/bust)
 *   2020: March crash 200%+ annualized, then 60-90%
 *   2021: Bull run 70-110% annualized
 *   2022: UST/LUNA crash 120%+, then 50-80% bear market
 *   2023: Recovery 40-60% annualized
 *   2024: ETF approval rally 50-80%, then normalization
 *   2025-2026: 40-70% base, spikes to 100%+ during events
 *
 * FIX 1 — structural stride-based filter in contractStart():
 *   CollateralVelocityModel computes velocity from a rolling window of LTV values
 *   accumulated during stateAt() calls. Because there is no prior LTV history at
 *   contractStart time, it is impossible to pre-evaluate velocity and skip callouts
 *   the way HealthFactorModel can skip by checking collateral prices.
 *   Instead, a STRIDE is applied: only every CALLOUT_STRIDE-th monitoring time is
 *   emitted as a PP callout. The stride is chosen so that the callout interval is
 *   still comfortably below the urgentDays threshold.
 *   With monitoringEventTimes at 1-min intervals and urgentDays=0.0208 (30 min):
 *     CALLOUT_STRIDE = 5 → callout every 5 min → 288 callouts for a 1-day window.
 *   With monitoringEventTimes at 15-min intervals (FIX 2 JSON):
 *     CALLOUT_STRIDE = 1 → callout every 15 min → 96 callouts (no further reduction needed).
 *   This keeps the total PP event count manageable while still catching velocity spikes.
 *
 * FIX 2 — reduced monitoringEventTimes density (JSON payload):
 *   Use 96 x 15-min points instead of 1440 x 1-min points for initial testing.
 *
 * Place this file in:
 *   src/main/java/org/actus/risksrv3/utils/defiliquidation1/
 */
public class CollateralVelocityModel implements BehaviorRiskModelProvider {

    public static final String CALLOUT_TYPE = "MRD";

    // FIX 1: stride for 1-minute resolution data.
    // Every 5th minute = 5-minute callout interval.
    // Still well below urgentDays=0.0208 (30 min), so no liquidation event is missed.
    // When using 15-min resolution data (FIX 2 JSON), stride=1 is used automatically
    // because the monitoringEventTimes are already spaced 15 minutes apart.
    private static final int CALLOUT_STRIDE = 5;

    private final String riskFactorId;
    private final String collateralPriceMOC;
    private final double collateralQuantity;
    private final double liquidationThreshold;     // e.g. 0.83
    private final double safeHorizonDays;          // e.g. 0.25 days = 6 hours
    private final double urgentDays;               // e.g. 0.0208 days = 30 minutes
    private final double moderateRepayFraction;    // e.g. 0.05
    private final double aggressiveRepayFraction;  // e.g. 0.15
    private final int rollingWindowSize;           // e.g. 30
    private final List<String> monitoringEventTimes;
    private final MultiMarketRiskModel marketModel;

    // Rolling LTV history for velocity computation — populated during stateAt() calls
    private final LinkedList<double[]> ltvHistory = new LinkedList<>(); // [0]=epoch_seconds, [1]=ltv

    public CollateralVelocityModel(String riskFactorId,
                                   CollateralVelocityModelData data,
                                   MultiMarketRiskModel marketModel) {
        this.riskFactorId           = riskFactorId;
        this.collateralPriceMOC     = data.getCollateralPriceMOC();
        this.collateralQuantity     = data.getCollateralQuantity();
        this.liquidationThreshold   = data.getLiquidationThreshold();
        this.safeHorizonDays        = data.getSafeHorizonDays();
        this.urgentDays             = data.getUrgentDays();
        this.moderateRepayFraction  = data.getModerateRepayFraction();
        this.aggressiveRepayFraction= data.getAggressiveRepayFraction();
        this.rollingWindowSize      = data.getRollingWindowSize();
        this.monitoringEventTimes   = data.getMonitoringEventTimes();
        this.marketModel            = marketModel;
    }

    @Override
    public Set<String> keys() {
        Set<String> keys = new HashSet<>();
        keys.add(this.riskFactorId);
        return keys;
    }

    @Override
    public List<CalloutData> contractStart(ContractModel contract) {
        // FIX 1 — stride-based filter.
        // Velocity cannot be pre-evaluated without a rolling LTV history, so we
        // use CALLOUT_STRIDE to thin the callout list instead of skipping by value.
        // Only every CALLOUT_STRIDE-th monitoring time is emitted as a PP callout.
        // The interval between callouts remains well below urgentDays so no
        // critical velocity event is missed.
        //
        // FIX 2: when the JSON payload already uses 15-min spacing, CALLOUT_STRIDE=5
        // would make callouts 75 minutes apart (too coarse for urgentDays=30min).
        // Therefore we auto-detect the spacing of the first two monitoring times and
        // cap the effective stride so the callout interval never exceeds urgentDays/2.

        LocalDateTime ied = contract.getAs("initialExchangeDate");

        // Auto-detect monitoring time spacing in minutes
        long spacingMinutes = 1; // default: assume 1-min data
        if (this.monitoringEventTimes.size() >= 2) {
            LocalDateTime t0 = LocalDateTime.parse(this.monitoringEventTimes.get(0));
            LocalDateTime t1 = LocalDateTime.parse(this.monitoringEventTimes.get(1));
            long diff = ChronoUnit.MINUTES.between(t0, t1);
            if (diff > 0) spacingMinutes = diff;
        }

        // urgentDays in minutes
        long urgentMinutes = Math.max(1L, (long)(this.urgentDays * 24 * 60));
        // Callout interval must be <= urgentDays/2 for adequate resolution
        long maxIntervalMinutes = Math.max(1L, urgentMinutes / 2);
        // Effective stride: how many monitoring steps to skip between callouts
        int effectiveStride = (int) Math.max(1, maxIntervalMinutes / spacingMinutes);
        // Cap at CALLOUT_STRIDE (don't emit MORE than every CALLOUT_STRIDE steps)
        effectiveStride = Math.min(effectiveStride, CALLOUT_STRIDE);

        List<CalloutData> callouts = new ArrayList<>();
        int skippedPreIed = 0;
        int skippedStride = 0;
        int included      = 0;
        int positionIndex = 0; // counts events after IED for stride modulo

        System.out.println("**** CollateralVelocityModel.contractStart [" + this.riskFactorId + "]:"
                + " spacingMin=" + spacingMinutes
                + " urgentMin=" + urgentMinutes
                + " effectiveStride=" + effectiveStride
                + " totalEvents=" + this.monitoringEventTimes.size());

        for (String eventTime : this.monitoringEventTimes) {
            LocalDateTime eventDateTime = LocalDateTime.parse(eventTime);

            // Guard 1: skip events before contract IED
            if (ied != null && eventDateTime.isBefore(ied)) {
                skippedPreIed++;
                continue;
            }

            // Guard 2 (FIX 1): stride filter — include only every effectiveStride-th event
            if (positionIndex % effectiveStride != 0) {
                skippedStride++;
                positionIndex++;
                continue;
            }

            callouts.add(new CalloutData(this.riskFactorId, eventTime, CALLOUT_TYPE));
            included++;
            positionIndex++;
        }

        System.out.println("**** CollateralVelocityModel.contractStart [" + this.riskFactorId + "]:"
                + " skippedPreIED=" + skippedPreIed
                + " skippedByStride=" + skippedStride
                + " callouts=" + included);
        return callouts;
    }

    @Override
    public double stateAt(String id, LocalDateTime time, StateSpace states) {

        double ethPrice = this.marketModel.stateAt(this.collateralPriceMOC, time);
        if (ethPrice <= 0.0) return 0.0;

        double collateralValue = this.collateralQuantity * ethPrice;
        double debt = states.notionalPrincipal + states.accruedInterest;
        if (debt <= 0.0) return 0.0;

        double currentLTV = debt / collateralValue;

        // Store in rolling window using epoch seconds for sub-day resolution
        // (the original used epochDay which loses intra-day granularity at minute level)
        long epochSeconds = time.toEpochSecond(java.time.ZoneOffset.UTC);
        ltvHistory.addLast(new double[]{epochSeconds, currentLTV});
        if (ltvHistory.size() > rollingWindowSize) {
            ltvHistory.removeFirst();
        }

        // Compute velocity (dLTV/dt in LTV-per-day)
        double velocity = 0.0;
        double daysToLiquidation = Double.MAX_VALUE;

        if (ltvHistory.size() >= 2) {
            double[] oldest = ltvHistory.getFirst();
            double[] newest = ltvHistory.getLast();
            // Convert epoch seconds difference to days
            double daysDiff = (newest[0] - oldest[0]) / 86400.0;
            double ltvDiff  = newest[1] - oldest[1];

            if (daysDiff > 0) {
                velocity = ltvDiff / daysDiff; // positive = deteriorating LTV

                if (velocity > 0.0) {
                    double ltvGap = this.liquidationThreshold - currentLTV;
                    if (ltvGap > 0) {
                        daysToLiquidation = ltvGap / velocity;
                    } else {
                        daysToLiquidation = 0.0; // already past threshold
                    }
                }
            }
        }

        System.out.println("**** CollateralVelocityModel: time=" + time
                + " LTV=" + String.format("%.4f", currentLTV)
                + " velocity=" + String.format("%.6f", velocity) + "/day"
                + " daysToLiq=" + (daysToLiquidation == Double.MAX_VALUE ? "MAX" : String.format("%.4f", daysToLiquidation))
                + " windowSize=" + ltvHistory.size());

        // Decision based on time-to-liquidation
        if (daysToLiquidation <= 0.0) {
            System.out.println("**** CollateralVelocityModel: PAST_THRESHOLD");
            return 1.0;
        } else if (daysToLiquidation <= urgentDays) {
            System.out.println("**** CollateralVelocityModel: URGENT daysToLiq="
                    + String.format("%.4f", daysToLiquidation));
            return aggressiveRepayFraction;
        } else if (daysToLiquidation <= safeHorizonDays) {
            System.out.println("**** CollateralVelocityModel: MODERATE daysToLiq="
                    + String.format("%.4f", daysToLiquidation));
            return moderateRepayFraction;
        } else {
            return 0.0;
        }
    }
}
