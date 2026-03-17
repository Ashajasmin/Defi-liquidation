/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.functions.lam;

import org.actus.functions.StateTransitionFunction;
import org.actus.states.StateSpace;
import org.actus.attributes.ContractModelProvider;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.conventions.daycount.DayCountCalculator;
import org.actus.conventions.businessday.BusinessDayAdjuster;

import java.time.LocalDateTime;

public final class STF_FP_LAM implements StateTransitionFunction {
    
    @Override
    public StateSpace eval(LocalDateTime time, StateSpace states,
    ContractModelProvider model, RiskFactorModelProvider riskFactorModel, DayCountCalculator dayCounter, BusinessDayAdjuster timeAdjuster) {
        // update state space
        double timeFromLastEvent = dayCounter.dayCountFraction(timeAdjuster.shiftCalcTime(states.statusDate), timeAdjuster.shiftCalcTime(time));
        states.accruedInterest += states.nominalInterestRate * states.interestCalculationBaseAmount * timeFromLastEvent;
        states.feeAccrued = 0.0;
        states.statusDate = time;

        // return post-event-states
        return StateSpace.copyStateSpace(states);
        }
    
}
