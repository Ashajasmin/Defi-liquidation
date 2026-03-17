package org.actus.risksrv3.utils.dynamicdiscounting1;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.dynamicdiscounting1.OptimalPaymentTimingModelData;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;
import org.actus.risksrv3.utils.MultiMarketRiskModel;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * OptimalPaymentTimingModel — Pre-computes the single optimal settlement day
 * that maximizes netBenefit = discountSavings - opportunityCost.
 * Only returns 1.0 on that exact day. Supports configurable discount functions.
 */
public class OptimalPaymentTimingModel implements BehaviorRiskModelProvider {

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
    private final double opportunityCostRate;
    private final List<String> monitoringEventTimes;
    private final MultiMarketRiskModel marketModel;
    private int optimalDay = -1;
    private boolean settled = false;

    public OptimalPaymentTimingModel(String riskFactorId,
                                     OptimalPaymentTimingModelData data,
                                     MultiMarketRiskModel marketModel) {
        this.riskFactorId = riskFactorId;
        this.invoiceDate = LocalDateTime.parse(data.getInvoiceDate(), FMT);
        this.dueDate = LocalDateTime.parse(data.getDueDate(), FMT);
        this.notionalAmount = data.getNotionalAmount();
        this.discountFunctionType = data.getDiscountFunctionType() != null ? data.getDiscountFunctionType() : "LINEAR";
        this.maxDiscountRate = data.getMaxDiscountRate() > 0 ? data.getMaxDiscountRate() : 0.02;
        this.decayLambda = data.getDecayLambda() > 0 ? data.getDecayLambda() : 3.0;
        this.powerAlpha = data.getPowerAlpha() > 0 ? data.getPowerAlpha() : 1.0;
        this.stepDiscountSchedule = data.getStepDiscountSchedule() != null ? new TreeMap<>(data.getStepDiscountSchedule()) : new TreeMap<>();
        this.opportunityCostRate = data.getOpportunityCostRate();
        this.monitoringEventTimes = data.getMonitoringEventTimes();
        this.marketModel = marketModel;
        precomputeOptimalDay();
    }

    private void precomputeOptimalDay() {
        long totalDays = ChronoUnit.DAYS.between(invoiceDate, dueDate);
        if (totalDays <= 0) { optimalDay = 0; return; }
        double bestBenefit = Double.NEGATIVE_INFINITY;
        for (int d = 0; d <= totalDays; d++) {
            double t = (double) d / totalDays;
            double discount = computeDiscountForDay(d, t);
            double savings = notionalAmount * discount;
            double oppCost = notionalAmount * opportunityCostRate * (totalDays - d);
            double benefit = savings - oppCost;
            if (benefit > bestBenefit) { bestBenefit = benefit; optimalDay = d; }
        }
        System.out.println("**** OptimalPaymentTimingModel: precomputed optimalDay=" + optimalDay
                + " of " + totalDays + " (function=" + discountFunctionType + ")");
    }

    private double computeDiscountForDay(int daysSince, double t) {
        switch (discountFunctionType.toUpperCase()) {
            case "LINEAR": return maxDiscountRate * (1.0 - t);
            case "STEPWISE": return lookupStep(daysSince);
            case "EXPONENTIAL": return (t >= 1.0) ? 0.0 : maxDiscountRate * Math.exp(-decayLambda * t);
            case "POWER": return maxDiscountRate * (1.0 - Math.pow(t, powerAlpha));
            default: return maxDiscountRate * (1.0 - t);
        }
    }

    private double lookupStep(int daysSince) {
        if (stepDiscountSchedule.isEmpty()) return maxDiscountRate;
        double rate = 0.0;
        for (Map.Entry<Integer, Double> e : stepDiscountSchedule.entrySet()) {
            if (daysSince >= e.getKey()) rate = e.getValue(); else break;
        }
        return rate;
    }

    @Override public Set<String> keys() { Set<String> k = new HashSet<>(); k.add(riskFactorId); return k; }
    @Override public List<CalloutData> contractStart(ContractModel contract) {
        // PP-before-IED fix: filter out callouts before contract starts
        LocalDateTime ied = contract.getAs("initialExchangeDate");
        List<CalloutData> c = new ArrayList<>();
        for (String t : monitoringEventTimes) {
            if (ied != null) {
                LocalDateTime eventDateTime = LocalDateTime.parse(t);
                if (eventDateTime.isBefore(ied)) {
                    System.out.println("**** OptimalPaymentTimingModel: SKIPPING pre-IED callout " + t + " (IED=" + ied + ")");
                    continue;
                }
            }
            c.add(new CalloutData(riskFactorId, t, CALLOUT_TYPE));
        }
        return c;
    }
    @Override public double stateAt(String id, LocalDateTime time, StateSpace states) {
        if (settled || states.notionalPrincipal <= 0.0) return 0.0;
        long daysSinceInvoice = ChronoUnit.DAYS.between(invoiceDate, time);
        if (daysSinceInvoice == optimalDay) {
            settled = true;
            System.out.println("**** OptimalPaymentTimingModel: SETTLE on optimalDay=" + optimalDay + " time=" + time);
            return 1.0;
        }
        return 0.0;
    }
}
