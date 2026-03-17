package org.actus.risksrv3.utils.dynamicdiscounting1;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.dynamicdiscounting1.SupplierUrgencyModelData;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;
import org.actus.risksrv3.utils.MultiMarketRiskModel;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * SupplierUrgencyModel — Adjusts discount based on supplier financial stress.
 * effectiveDiscount = baseDiscount × (1 + stressMultiplier × supplierStress)
 * When stress=0.8, stressMultiplier=2.0: 2% base → 2% × (1 + 2×0.8) = 5.2%
 * Supports configurable discount functions.
 */
public class SupplierUrgencyModel implements BehaviorRiskModelProvider {

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
    private final double stressMultiplier;
    private final String supplierStressMOC;
    private final double hurdleRateAnnualized;
    private final List<String> monitoringEventTimes;
    private final MultiMarketRiskModel marketModel;
    private boolean settled = false;

    public SupplierUrgencyModel(String riskFactorId, SupplierUrgencyModelData data, MultiMarketRiskModel marketModel) {
        this.riskFactorId = riskFactorId;
        this.invoiceDate = LocalDateTime.parse(data.getInvoiceDate(), FMT);
        this.dueDate = LocalDateTime.parse(data.getDueDate(), FMT);
        this.notionalAmount = data.getNotionalAmount();
        this.discountFunctionType = data.getDiscountFunctionType() != null ? data.getDiscountFunctionType() : "LINEAR";
        this.maxDiscountRate = data.getMaxDiscountRate() > 0 ? data.getMaxDiscountRate() : 0.02;
        this.decayLambda = data.getDecayLambda() > 0 ? data.getDecayLambda() : 3.0;
        this.powerAlpha = data.getPowerAlpha() > 0 ? data.getPowerAlpha() : 1.0;
        this.stepDiscountSchedule = data.getStepDiscountSchedule() != null ? new TreeMap<>(data.getStepDiscountSchedule()) : new TreeMap<>();
        this.stressMultiplier = data.getStressMultiplier() > 0 ? data.getStressMultiplier() : 2.0;
        this.supplierStressMOC = data.getSupplierStressMOC();
        this.hurdleRateAnnualized = data.getHurdleRateAnnualized();
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
                    System.out.println("**** SupplierUrgencyModel: SKIPPING pre-IED callout " + t + " (IED=" + ied + ")");
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
        if (totalDays <= 0) return 0.0;
        double t = Math.min(1.0, (double) daysSince / totalDays);
        double baseDiscount;
        switch (discountFunctionType.toUpperCase()) {
            case "LINEAR": baseDiscount = maxDiscountRate * (1.0 - t); break;
            case "EXPONENTIAL": baseDiscount = (t >= 1.0) ? 0.0 : maxDiscountRate * Math.exp(-decayLambda * t); break;
            case "POWER": baseDiscount = maxDiscountRate * (1.0 - Math.pow(t, powerAlpha)); break;
            default: baseDiscount = maxDiscountRate * (1.0 - t);
        }
        double supplierStress = (supplierStressMOC != null && !supplierStressMOC.isEmpty()) ? marketModel.stateAt(supplierStressMOC, time) : 0.0;
        double effectiveDiscount = baseDiscount * (1.0 + stressMultiplier * supplierStress);
        long daysRemaining = ChronoUnit.DAYS.between(time, dueDate);
        double apr = (daysRemaining > 0) ? effectiveDiscount * 365.0 / daysRemaining : 0.0;
        System.out.println("**** SupplierUrgencyModel: time=" + time + " baseDiscount=" + String.format("%.4f%%", baseDiscount * 100) + " stress=" + String.format("%.2f", supplierStress) + " effectiveDiscount=" + String.format("%.4f%%", effectiveDiscount * 100) + " APR=" + String.format("%.2f%%", apr * 100));
        if (apr < hurdleRateAnnualized && daysRemaining > 0) return 0.0;
        settled = true;
        System.out.println("**** SupplierUrgencyModel: SETTLEMENT at time=" + time);
        return 1.0;
    }
}
