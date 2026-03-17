package org.actus.risksrv3.utils.dynamicdiscounting1;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.dynamicdiscounting1.CashPoolOptimizationModelData;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;
import org.actus.risksrv3.utils.MultiMarketRiskModel;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * CashPoolOptimizationModel — Portfolio-level cash allocation.
 * Ranks this invoice by APR × supplierCriticality, settles if cash is allocated.
 * Supports configurable discount functions.
 */
public class CashPoolOptimizationModel implements BehaviorRiskModelProvider {

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
    private final double supplierCriticality;
    private final String buyerCashMOC;
    private final double portfolioAllocatedCash;
    private final List<String> monitoringEventTimes;
    private final MultiMarketRiskModel marketModel;
    private boolean settled = false;

    public CashPoolOptimizationModel(String riskFactorId, CashPoolOptimizationModelData data, MultiMarketRiskModel marketModel) {
        this.riskFactorId = riskFactorId;
        this.invoiceDate = LocalDateTime.parse(data.getInvoiceDate(), FMT);
        this.dueDate = LocalDateTime.parse(data.getDueDate(), FMT);
        this.notionalAmount = data.getNotionalAmount();
        this.discountFunctionType = data.getDiscountFunctionType() != null ? data.getDiscountFunctionType() : "LINEAR";
        this.maxDiscountRate = data.getMaxDiscountRate() > 0 ? data.getMaxDiscountRate() : 0.02;
        this.decayLambda = data.getDecayLambda() > 0 ? data.getDecayLambda() : 3.0;
        this.powerAlpha = data.getPowerAlpha() > 0 ? data.getPowerAlpha() : 1.0;
        this.stepDiscountSchedule = data.getStepDiscountSchedule() != null ? new TreeMap<>(data.getStepDiscountSchedule()) : new TreeMap<>();
        this.supplierCriticality = data.getSupplierCriticality();
        this.buyerCashMOC = data.getBuyerCashMOC();
        this.portfolioAllocatedCash = data.getPortfolioAllocatedCash();
        this.monitoringEventTimes = data.getMonitoringEventTimes();
        this.marketModel = marketModel;
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
                    System.out.println("**** CashPoolOptimizationModel: SKIPPING pre-IED callout " + t + " (IED=" + ied + ")");
                    continue;
                }
            }
            c.add(new CalloutData(riskFactorId, t, CALLOUT_TYPE));
        }
        return c;
    }
    @Override public double stateAt(String id, LocalDateTime time, StateSpace states) {
        if (settled || states.notionalPrincipal <= 0.0 || time.isAfter(dueDate)) return 0.0;
        long daysSince = ChronoUnit.DAYS.between(invoiceDate, time);
        long totalDays = ChronoUnit.DAYS.between(invoiceDate, dueDate);
        long daysRemaining = ChronoUnit.DAYS.between(time, dueDate);
        if (totalDays <= 0 || daysRemaining <= 0) return 0.0;
        double t = (double) daysSince / totalDays;
        double discount;
        switch (discountFunctionType.toUpperCase()) {
            case "LINEAR": discount = maxDiscountRate * (1.0 - t); break;
            case "EXPONENTIAL": discount = maxDiscountRate * Math.exp(-decayLambda * t); break;
            case "POWER": discount = maxDiscountRate * (1.0 - Math.pow(t, powerAlpha)); break;
            default: discount = maxDiscountRate * (1.0 - t);
        }
        double apr = discount * 365.0 / daysRemaining;
        double priorityScore = apr * supplierCriticality;
        double settlementAmount = notionalAmount * (1.0 - discount);
        double availableCash = portfolioAllocatedCash;
        if (buyerCashMOC != null && !buyerCashMOC.isEmpty()) {
            availableCash = Math.min(portfolioAllocatedCash, marketModel.stateAt(buyerCashMOC, time));
        }
        System.out.println("**** CashPoolOptimizationModel: time=" + time + " APR=" + String.format("%.2f%%", apr*100) + " criticality=" + String.format("%.2f", supplierCriticality) + " priority=" + String.format("%.4f", priorityScore) + " need=$" + String.format("%.2f", settlementAmount) + " allocated=$" + String.format("%.2f", availableCash));
        if (availableCash >= settlementAmount && priorityScore > 0.0) {
            settled = true;
            System.out.println("**** CashPoolOptimizationModel: SETTLE priority=" + String.format("%.4f", priorityScore));
            return 1.0;
        }
        return 0.0;
    }
}
