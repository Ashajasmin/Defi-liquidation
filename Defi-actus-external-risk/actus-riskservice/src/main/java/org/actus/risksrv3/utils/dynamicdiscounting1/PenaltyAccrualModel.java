package org.actus.risksrv3.utils.dynamicdiscounting1;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.dynamicdiscounting1.PenaltyAccrualModelData;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;
import org.actus.risksrv3.utils.MultiMarketRiskModel;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * PenaltyAccrualModel — Post-due-date penalty computation with configurable function.
 *
 * Configurable penalty functions (default: STEPWISE):
 *   LINEAR:      penalty = notional × basePenaltyRate × daysOverdue
 *   STEPWISE:    penalty = notional × stepRate(daysOverdue) × daysOverdue
 *   EXPONENTIAL: penalty = notional × basePenaltyRate × daysOverdue × exp(λ × daysOverdue/horizon)
 *   POWER:       penalty = notional × basePenaltyRate × daysOverdue^α
 *   CUSTOM:      penaltyRate from market model, penalty = notional × customRate × daysOverdue
 *
 * Default stepSchedule: {0:0.0005, 16:0.00075, 31:0.001, 61:0.00125, 91:0.0015}
 * Delinquency alerts fire at days 15, 30, 60, 90 overdue.
 * All penalties use SIMPLE interest (no compounding).
 * Returns 0.0 (informational only — does NOT trigger settlement).
 */
public class PenaltyAccrualModel implements BehaviorRiskModelProvider {

    public static final String CALLOUT_TYPE = "MRD";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final String riskFactorId;
    private final LocalDateTime dueDate;
    private final String penaltyFunctionType;
    private final double basePenaltyRate;
    private final double penaltyGrowthLambda;
    private final double penaltyPowerAlpha;
    private final TreeMap<Integer, Double> penaltyStepSchedule;
    private final String penaltyRateMOC;
    private final int penaltyHorizonDays;
    private final List<String> monitoringEventTimes;
    private final MultiMarketRiskModel marketModel;

    private static final TreeMap<Integer, Double> DEFAULT_STEP_SCHEDULE = new TreeMap<>();
    static {
        DEFAULT_STEP_SCHEDULE.put(0, 0.0005);
        DEFAULT_STEP_SCHEDULE.put(16, 0.00075);
        DEFAULT_STEP_SCHEDULE.put(31, 0.001);
        DEFAULT_STEP_SCHEDULE.put(61, 0.00125);
        DEFAULT_STEP_SCHEDULE.put(91, 0.0015);
    }

    public PenaltyAccrualModel(String riskFactorId,
                               PenaltyAccrualModelData data,
                               MultiMarketRiskModel marketModel) {
        this.riskFactorId = riskFactorId;
        this.dueDate = LocalDateTime.parse(data.getDueDate(), FMT);
        this.penaltyFunctionType = data.getPenaltyFunctionType() != null
                ? data.getPenaltyFunctionType() : "STEPWISE";
        this.basePenaltyRate = data.getBasePenaltyRate() > 0
                ? data.getBasePenaltyRate() : 0.0005;
        this.penaltyGrowthLambda = data.getPenaltyGrowthLambda() > 0
                ? data.getPenaltyGrowthLambda() : 2.0;
        this.penaltyPowerAlpha = data.getPenaltyPowerAlpha() > 0
                ? data.getPenaltyPowerAlpha() : 1.5;
        this.penaltyStepSchedule = data.getPenaltyStepSchedule() != null
                ? new TreeMap<>(data.getPenaltyStepSchedule()) : new TreeMap<>(DEFAULT_STEP_SCHEDULE);
        this.penaltyRateMOC = data.getPenaltyRateMOC();
        this.penaltyHorizonDays = data.getPenaltyHorizonDays() > 0
                ? data.getPenaltyHorizonDays() : 180;
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
        // PP-before-IED fix: filter out callouts before contract starts
        LocalDateTime ied = contract.getAs("initialExchangeDate");
        List<CalloutData> callouts = new ArrayList<>();
        for (String eventTime : this.monitoringEventTimes) {
            if (ied != null) {
                LocalDateTime eventDateTime = LocalDateTime.parse(eventTime);
                if (eventDateTime.isBefore(ied)) {
                    System.out.println("**** PenaltyAccrualModel: SKIPPING pre-IED callout " + eventTime + " (IED=" + ied + ")");
                    continue;
                }
            }
            callouts.add(new CalloutData(this.riskFactorId, eventTime, CALLOUT_TYPE));
        }
        return callouts;
    }

    @Override
    public double stateAt(String id, LocalDateTime time, StateSpace states) {
        if (!time.isAfter(dueDate)) return 0.0;
        if (states.notionalPrincipal <= 0.0) return 0.0;

        long daysOverdue = ChronoUnit.DAYS.between(dueDate, time);
        double notional = states.notionalPrincipal;
        double penaltyAmount = computePenalty(daysOverdue, notional, time);
        double totalDue = notional + penaltyAmount;
        String alertLevel = getAlertLevel(daysOverdue);

        System.out.println("**** PenaltyAccrualModel: [" + alertLevel + "] time=" + time
                + " daysOverdue=" + daysOverdue
                + " function=" + penaltyFunctionType
                + " penalty=$" + String.format("%.2f", penaltyAmount)
                + " totalDue=$" + String.format("%.2f", totalDue));

        if (daysOverdue == 15 || daysOverdue == 30 || daysOverdue == 60 || daysOverdue == 90) {
            System.out.println("**** DELINQUENCY ALERT: " + id
                    + " | " + daysOverdue + " DAYS OVERDUE"
                    + " | Level: " + alertLevel
                    + " | Penalty: $" + String.format("%.2f", penaltyAmount)
                    + " | Total Due: $" + String.format("%.2f", totalDue));
        }

        return 0.0;
    }

    private double computePenalty(long daysOverdue, double notional, LocalDateTime time) {
        if (daysOverdue <= 0) return 0.0;

        switch (penaltyFunctionType.toUpperCase()) {
            case "LINEAR":
                return notional * basePenaltyRate * daysOverdue;
            case "STEPWISE":
                double stepRate = lookupStepRate(daysOverdue);
                return notional * stepRate * daysOverdue;
            case "EXPONENTIAL":
                double horizon = (penaltyHorizonDays > 0) ? penaltyHorizonDays : 180.0;
                double expFactor = Math.exp(penaltyGrowthLambda * daysOverdue / horizon);
                return notional * basePenaltyRate * daysOverdue * expFactor;
            case "POWER":
                double alpha = (penaltyPowerAlpha > 0) ? penaltyPowerAlpha : 1.0;
                return notional * basePenaltyRate * Math.pow(daysOverdue, alpha);
            case "CUSTOM":
                if (penaltyRateMOC != null && !penaltyRateMOC.isEmpty()) {
                    double customRate = marketModel.stateAt(penaltyRateMOC, time);
                    return notional * customRate * daysOverdue;
                }
                return notional * basePenaltyRate * daysOverdue;
            default:
                double defRate = lookupStepRate(daysOverdue);
                return notional * defRate * daysOverdue;
        }
    }

    private double lookupStepRate(long daysOverdue) {
        double rate = basePenaltyRate;
        for (Map.Entry<Integer, Double> entry : penaltyStepSchedule.entrySet()) {
            if (daysOverdue >= entry.getKey()) {
                rate = entry.getValue();
            } else {
                break;
            }
        }
        return rate;
    }

    private String getAlertLevel(long daysOverdue) {
        if (daysOverdue <= 15) return "GRACE";
        if (daysOverdue <= 30) return "15-DAY";
        if (daysOverdue <= 60) return "30-DAY";
        if (daysOverdue <= 90) return "60-DAY";
        return "90-DAY+";
    }
}
