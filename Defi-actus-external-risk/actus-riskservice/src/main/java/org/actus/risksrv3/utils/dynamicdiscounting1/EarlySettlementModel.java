package org.actus.risksrv3.utils.dynamicdiscounting1;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.dynamicdiscounting1.EarlySettlementModelData;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;
import org.actus.risksrv3.utils.MultiMarketRiskModel;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * EarlySettlementModel — Core dynamic discounting behavioral model.
 *
 * Decides WHEN to settle an invoice early based on configurable discount curve,
 * buyer cash availability, and hurdle rate APR.
 *
 * Configurable discount functions (default: LINEAR, maxDiscountRate: 0.02):
 *   LINEAR:      discount(d) = maxDiscount × (1 - d/totalDays)
 *   STEPWISE:    discount(d) = lookup from stepDiscountSchedule
 *   EXPONENTIAL: discount(d) = maxDiscount × exp(-λ × d/totalDays)
 *   POWER:       discount(d) = maxDiscount × (1 - (d/totalDays)^α)
 *   CUSTOM:      discount(d) = marketModel.stateAt(customDiscountMOC, time)
 *
 * Returns 1.0 to trigger PP (prepayment) event when conditions are met.
 */
public class EarlySettlementModel implements BehaviorRiskModelProvider {

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
    private final String customDiscountMOC;
    private final double hurdleRateAnnualized;
    private final String buyerCashMOC;
    private final List<String> monitoringEventTimes;
    private final MultiMarketRiskModel marketModel;
    private boolean settled = false;

    // Dual-call tracking: ACTUS engine calls stateAt twice per timestamp
    //   1st call (POF_PP_rf2): needs discountedFraction → payoff = fraction × notional
    //   2nd call (STF_PP_rf2): needs 1.0 → notional -= 1.0 × notional = 0 (full cancellation)
    // For non-settlement: both calls return 0.0 (no payoff, no notional change)
    private LocalDateTime lastCalledTime = null;
    private int callCountAtTime = 0;
    private double settlementFraction = 0.0;

    public EarlySettlementModel(String riskFactorId,
                                EarlySettlementModelData data,
                                MultiMarketRiskModel marketModel) {
        this.riskFactorId = riskFactorId;
        this.invoiceDate = LocalDateTime.parse(data.getInvoiceDate(), FMT);
        this.dueDate = LocalDateTime.parse(data.getDueDate(), FMT);
        this.notionalAmount = data.getNotionalAmount();
        this.discountFunctionType = data.getDiscountFunctionType() != null
                ? data.getDiscountFunctionType() : "LINEAR";
        this.maxDiscountRate = data.getMaxDiscountRate() > 0
                ? data.getMaxDiscountRate() : 0.02;
        this.decayLambda = data.getDecayLambda() > 0 ? data.getDecayLambda() : 3.0;
        this.powerAlpha = data.getPowerAlpha() > 0 ? data.getPowerAlpha() : 1.0;
        this.stepDiscountSchedule = data.getStepDiscountSchedule() != null
                ? new TreeMap<>(data.getStepDiscountSchedule()) : new TreeMap<>();
        this.customDiscountMOC = data.getCustomDiscountMOC();
        this.hurdleRateAnnualized = data.getHurdleRateAnnualized();
        this.buyerCashMOC = data.getBuyerCashMOC();
        this.monitoringEventTimes = data.getMonitoringEventTimes();
        this.marketModel = marketModel;
    }

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
            if (ied != null) { LocalDateTime edt = LocalDateTime.parse(eventTime); if (edt.isBefore(ied)) { System.out.println("**** EarlySettlementModel: SKIPPING pre-IED callout " + eventTime + " (IED=" + ied + ")"); continue; } }
            callouts.add(new CalloutData(this.riskFactorId, eventTime, CALLOUT_TYPE));
        }
        return callouts;
    }

    @Override
    public double stateAt(String id, LocalDateTime time, StateSpace states) {
        // Track call count: ACTUS engine calls stateAt twice per PP event at same timestamp
        //   Call 1 (from POF_PP_rf2): we return discountedFraction for payoff calculation
        //   Call 2 (from STF_PP_rf2): we return 1.0 so notional -= 1.0 × notional = 0
        if (lastCalledTime != null && lastCalledTime.equals(time)) {
            callCountAtTime++;
        } else {
            lastCalledTime = time;
            callCountAtTime = 1;
            settlementFraction = 0.0;
        }

        // 2nd call at same timestamp: STF needs the full-cancellation signal
        if (callCountAtTime == 2) {
            System.out.println("**** EarlySettlementModel: STF call time=" + time
                    + " returning=" + (settlementFraction > 0.0 ? "1.0 (full cancellation)" : "0.0"));
            return (settlementFraction > 0.0) ? 1.0 : 0.0;
        }

        // 1st call: evaluate settlement decision (POF path)
        if (settled || states.notionalPrincipal <= 0.0) return 0.0;
        if (time.isAfter(dueDate)) return 0.0;

        double discount = computeDiscount(time);
        long daysRemaining = ChronoUnit.DAYS.between(time, dueDate);
        double apr = (daysRemaining > 0) ? discount * 365.0 / daysRemaining : 0.0;

        if (apr < hurdleRateAnnualized && daysRemaining > 0) {
            System.out.println("**** EarlySettlementModel: time=" + time
                    + " discount=" + String.format("%.4f%%", discount * 100)
                    + " APR=" + String.format("%.2f%%", apr * 100)
                    + " < hurdle=" + String.format("%.2f%%", hurdleRateAnnualized * 100)
                    + " → WAIT");
            return 0.0;
        }

        if (buyerCashMOC != null && !buyerCashMOC.isEmpty()) {
            double buyerCash = marketModel.stateAt(buyerCashMOC, time);
            double settlementAmount = notionalAmount * (1.0 - discount);
            if (buyerCash < settlementAmount) {
                System.out.println("**** EarlySettlementModel: time=" + time
                        + " INSUFFICIENT_CASH: need=$" + String.format("%.2f", settlementAmount)
                        + " have=$" + String.format("%.2f", buyerCash));
                return 0.0;
            }
        }

        // Settlement triggered!
        settled = true;
        double discountedFraction = 1.0 - discount;
        settlementFraction = discountedFraction;  // cache for STF call
        double savings = notionalAmount * discount;
        double netPayment = notionalAmount * discountedFraction;
        System.out.println("**** EarlySettlementModel: SETTLEMENT at time=" + time
                + " function=" + discountFunctionType
                + " discount=" + String.format("%.4f%%", discount * 100)
                + " savings=$" + String.format("%.2f", savings)
                + " netPayment=$" + String.format("%.2f", netPayment)
                + " discountedFraction=" + String.format("%.6f", discountedFraction)
                + " APR=" + String.format("%.2f%%", apr * 100));

        // POF_PP_rf2 computes: payoff = discountedFraction × notionalPrincipal
        return discountedFraction;
    }

    private double computeDiscount(LocalDateTime time) {
        long daysSince = ChronoUnit.DAYS.between(invoiceDate, time);
        long totalDays = ChronoUnit.DAYS.between(invoiceDate, dueDate);
        if (totalDays <= 0) return 0.0;
        double t = Math.min(1.0, (double) daysSince / totalDays);

        switch (discountFunctionType.toUpperCase()) {
            case "LINEAR":
                return maxDiscountRate * (1.0 - t);
            case "STEPWISE":
                return lookupStepDiscount((int) daysSince);
            case "EXPONENTIAL":
                double expVal = maxDiscountRate * Math.exp(-decayLambda * t);
                return (t >= 1.0) ? 0.0 : expVal;
            case "POWER":
                return maxDiscountRate * (1.0 - Math.pow(t, powerAlpha));
            case "CUSTOM":
                if (customDiscountMOC != null && !customDiscountMOC.isEmpty()) {
                    return Math.abs(marketModel.stateAt(customDiscountMOC, time));
                }
                return maxDiscountRate * (1.0 - t);
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
