package org.actus.risksrv3.utils.dynamicdiscounting1;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.dynamicdiscounting1.FactoringDecisionModelData;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;
import org.actus.risksrv3.utils.MultiMarketRiskModel;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * FactoringDecisionModel — Compares 3 financing channels:
 *   1. Dynamic discount (configurable function)
 *   2. Bank factoring (fixed APR)
 *   3. Reverse factoring (fixed APR)
 *
 * Uses the same dual-call pattern as EarlySettlementModel:
 *   Call 1 (POF_PP_rf2): returns discountedFraction → payoff = fraction × notional
 *   Call 2 (STF_PP_rf2): returns 1.0 → notional -= 1.0 × notional = 0 (full cancellation)
 * If discount is NOT cheapest: both calls return 0.0
 */
public class FactoringDecisionModel implements BehaviorRiskModelProvider {

    public static final String CALLOUT_TYPE = "MRD";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final String riskFactorId;
    private final LocalDateTime invoiceDate;
    private final LocalDateTime dueDate;
    private final double notionalAmount;
    private final String discountFunctionType;
    private final double maxDiscountRate;
    private final double decayLambda;
    private final double powerAlpha;
    private final TreeMap<Integer, Double> stepDiscountSchedule;
    private final double bankFactoringRateAnnualized;
    private final double reverseFactoringRateAnnualized;
    private final List<String> monitoringEventTimes;
    private final MultiMarketRiskModel marketModel;
    private boolean settled = false;

    // Dual-call tracking: ACTUS engine calls stateAt twice per PP event
    //   1st call (POF_PP_rf2): return discountedFraction → payoff = fraction × notional
    //   2nd call (STF_PP_rf2): return 1.0 → notional -= 1.0 × notional = 0
    private LocalDateTime lastCalledTime = null;
    private int callCountAtTime = 0;
    private double settlementFraction = 0.0;

    public FactoringDecisionModel(String riskFactorId, FactoringDecisionModelData data, MultiMarketRiskModel marketModel) {
        this.riskFactorId = riskFactorId;
        this.invoiceDate = LocalDateTime.parse(data.getInvoiceDate(), FMT);
        this.dueDate = LocalDateTime.parse(data.getDueDate(), FMT);
        this.notionalAmount = data.getNotionalAmount();
        this.discountFunctionType = data.getDiscountFunctionType() != null ? data.getDiscountFunctionType() : "LINEAR";
        this.maxDiscountRate = data.getMaxDiscountRate() > 0 ? data.getMaxDiscountRate() : 0.02;
        this.decayLambda = data.getDecayLambda() > 0 ? data.getDecayLambda() : 3.0;
        this.powerAlpha = data.getPowerAlpha() > 0 ? data.getPowerAlpha() : 1.0;
        this.stepDiscountSchedule = data.getStepDiscountSchedule() != null ? new TreeMap<>(data.getStepDiscountSchedule()) : new TreeMap<>();
        this.bankFactoringRateAnnualized = data.getBankFactoringRateAnnualized();
        this.reverseFactoringRateAnnualized = data.getReverseFactoringRateAnnualized();
        this.monitoringEventTimes = data.getMonitoringEventTimes();
        this.marketModel = marketModel;
    }

    @Override
    public Set<String> keys() {
        Set<String> k = new HashSet<>();
        k.add(riskFactorId);
        return k;
    }

    @Override
    public List<CalloutData> contractStart(ContractModel contract) {
        // PP-before-IED fix: filter out callouts before contract starts
        LocalDateTime ied = contract.getAs("initialExchangeDate");
        List<CalloutData> c = new ArrayList<>();
        for (String t : monitoringEventTimes) {
            if (ied != null) {
                LocalDateTime eventDateTime = LocalDateTime.parse(t);
                if (eventDateTime.isBefore(ied)) {
                    System.out.println("**** FactoringDecisionModel: SKIPPING pre-IED callout " + t + " (IED=" + ied + ")");
                    continue;
                }
            }
            c.add(new CalloutData(riskFactorId, t, CALLOUT_TYPE));
        }
        return c;
    }

    @Override
    public double stateAt(String id, LocalDateTime time, StateSpace states) {
        // Track call count: ACTUS engine calls stateAt twice per PP event
        if (lastCalledTime != null && lastCalledTime.equals(time)) {
            callCountAtTime++;
        } else {
            lastCalledTime = time;
            callCountAtTime = 1;
            settlementFraction = 0.0;
        }

        // 2nd call at same timestamp: STF needs full-cancellation signal
        if (callCountAtTime == 2) {
            System.out.println("**** FactoringDecisionModel: STF call time=" + time
                    + " returning=" + (settlementFraction > 0.0 ? "1.0 (full cancellation)" : "0.0"));
            return (settlementFraction > 0.0) ? 1.0 : 0.0;
        }

        // 1st call: evaluate channel comparison (POF path)
        if (settled || states.notionalPrincipal <= 0.0 || time.isAfter(dueDate)) return 0.0;

        long daysSince = ChronoUnit.DAYS.between(invoiceDate, time);
        long totalDays = ChronoUnit.DAYS.between(invoiceDate, dueDate);
        long daysRemaining = ChronoUnit.DAYS.between(time, dueDate);
        if (totalDays <= 0 || daysRemaining <= 0) return 0.0;

        double t = (double) daysSince / totalDays;
        double discount = computeDiscount(t, daysSince);
        double dynDiscAPR = discount * 365.0 / daysRemaining;

        // Compute absolute costs for each channel
        double bankCost = notionalAmount * bankFactoringRateAnnualized * daysRemaining / 365.0;
        double reverseCost = notionalAmount * reverseFactoringRateAnnualized * daysRemaining / 365.0;
        double dynDiscCost = notionalAmount * discount;

        // Find cheapest channel
        String cheapest = "BANK";
        double cheapestCost = bankCost;
        if (reverseCost < cheapestCost) { cheapest = "REVERSE"; cheapestCost = reverseCost; }
        if (dynDiscCost < cheapestCost) { cheapest = "DYNDISC"; cheapestCost = dynDiscCost; }

        System.out.println("**** FactoringDecisionModel: time=" + time
                + " dynDisc=$" + String.format("%.2f", dynDiscCost)
                + "(APR=" + String.format("%.2f%%", dynDiscAPR * 100) + ")"
                + " bank=$" + String.format("%.2f", bankCost)
                + " reverse=$" + String.format("%.2f", reverseCost)
                + " -> " + cheapest);

        if ("DYNDISC".equals(cheapest)) {
            // Dynamic discount wins — trigger settlement
            settled = true;
            double discountedFraction = 1.0 - discount;
            settlementFraction = discountedFraction;  // cache for STF call

            System.out.println("**** FactoringDecisionModel: SETTLEMENT at time=" + time
                    + " discount=" + String.format("%.4f%%", discount * 100)
                    + " savings=$" + String.format("%.2f", notionalAmount * discount)
                    + " netPayment=$" + String.format("%.2f", notionalAmount * discountedFraction)
                    + " discountedFraction=" + String.format("%.6f", discountedFraction));

            // POF_PP_rf2 computes: payoff = discountedFraction × notionalPrincipal
            return discountedFraction;
        }

        return 0.0;
    }

    private double computeDiscount(double t, long daysSince) {
        switch (discountFunctionType.toUpperCase()) {
            case "LINEAR":
                return maxDiscountRate * (1.0 - t);
            case "STEPWISE":
                return lookupStepDiscount((int) daysSince);
            case "EXPONENTIAL":
                return (t >= 1.0) ? 0.0 : maxDiscountRate * Math.exp(-decayLambda * t);
            case "POWER":
                return maxDiscountRate * (1.0 - Math.pow(t, powerAlpha));
            default:
                return maxDiscountRate * (1.0 - t);
        }
    }

    private double lookupStepDiscount(int daysSince) {
        if (stepDiscountSchedule.isEmpty()) return maxDiscountRate;
        double rate = 0.0;
        for (Map.Entry<Integer, Double> entry : stepDiscountSchedule.entrySet()) {
            if (daysSince >= entry.getKey()) {
                rate = entry.getValue();
            } else {
                break;
            }
        }
        return rate;
    }
}
