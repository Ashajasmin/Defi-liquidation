/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.functions.swppv;

import org.actus.functions.StateTransitionFunction;
import org.actus.states.StateSpace;
import org.actus.attributes.ContractModelProvider;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.conventions.daycount.DayCountCalculator;
import org.actus.conventions.businessday.BusinessDayAdjuster;
import org.actus.types.ContractPerformance;

import java.time.LocalDateTime;

public final class STF_CD_SWPPV implements StateTransitionFunction {
    
    @Override
    public StateSpace eval(LocalDateTime time, StateSpace states,
    ContractModelProvider model, RiskFactorModelProvider riskFactorModel, DayCountCalculator dayCounter, BusinessDayAdjuster timeAdjuster) {

        // update state space
        double timeFromLastEvent = dayCounter.dayCountFraction(timeAdjuster.shiftCalcTime(states.statusDate), timeAdjuster.shiftCalcTime(time));
        states.accruedInterest += (model.<Double>getAs("nominalInterestRate") - states.nominalInterestRate) * states.notionalPrincipal * timeFromLastEvent;
        states.accruedInterest += model.<Double>getAs("nominalInterestRate") * states.notionalPrincipal * timeFromLastEvent;
        states.accruedInterest2 += (-1) * states.nominalInterestRate * states.notionalPrincipal * timeFromLastEvent;
        states.contractPerformance = ContractPerformance.DF;
        states.statusDate = time;

        // return post-event-states
        return StateSpace.copyStateSpace(states);
    }
    
}
