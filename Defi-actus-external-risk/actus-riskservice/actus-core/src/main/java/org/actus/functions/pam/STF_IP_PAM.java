/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.functions.pam;

import org.actus.conventions.contractrole.ContractRoleConvention;
import org.actus.functions.StateTransitionFunction;
import org.actus.states.StateSpace;
import org.actus.attributes.ContractModelProvider;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.conventions.daycount.DayCountCalculator;
import org.actus.conventions.businessday.BusinessDayAdjuster;
import org.actus.time.ScheduleFactory;
import org.actus.types.ContractRole;
import org.actus.types.FeeBasis;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public final class STF_IP_PAM implements StateTransitionFunction {
    
    @Override
    public StateSpace eval(LocalDateTime time, StateSpace states,
    ContractModelProvider model, RiskFactorModelProvider riskFactorModel, DayCountCalculator dayCounter, BusinessDayAdjuster timeAdjuster) {
        // update state space
        states.accruedInterest = 0.0;

        // update fee-accrued
        //if(FeeBasis.N.equals(model.<FeeBasis>getAs("feeBasis"))) {
        states.feeAccrued += dayCounter.dayCountFraction(timeAdjuster.shiftCalcTime(states.statusDate), timeAdjuster.shiftCalcTime(time))
                    * model.<Double>getAs("feeRate")
                    * states.notionalPrincipal;
        /*}else {
            Set<LocalDateTime> feePaymentSchedule = ScheduleFactory.createSchedule(
                    model.getAs("cycleAnchorDateOfFee"),
                    states.maturityDate,
                    model.getAs("cycleOfFee"),
                    model.getAs("endOfMonthConvention")
            );
            List<LocalDateTime> feePaymentList = new ArrayList<>(feePaymentSchedule);
            Collections.sort(feePaymentList);
            LocalDateTime prevTimePoint = null;
            LocalDateTime nextTimePoint = null;
            Iterator<LocalDateTime> schedule = feePaymentList.iterator();
            while(schedule.hasNext()){
                LocalDateTime timePoint = schedule.next();
                if(timePoint.isBefore(time)){
                    prevTimePoint = timePoint;
                }
                if(timePoint.isAfter(time)){
                    nextTimePoint = timePoint;
                    break;
                }
            }
            states.feeAccrued += (dayCounter.dayCountFraction(timeAdjuster.shiftCalcTime(prevTimePoint), timeAdjuster.shiftCalcTime(time))
                    / dayCounter.dayCountFraction(timeAdjuster.shiftCalcTime(prevTimePoint), timeAdjuster.shiftCalcTime(nextTimePoint)))
                    * ContractRoleConvention.roleSign(model.<ContractRole>getAs("contractRole"))
                    * model.<Double>getAs("feeRate");
        }*/
        //update status date
        states.statusDate = time;
        
        // copy post-event-states
        return StateSpace.copyStateSpace(states);
        }
    
}
