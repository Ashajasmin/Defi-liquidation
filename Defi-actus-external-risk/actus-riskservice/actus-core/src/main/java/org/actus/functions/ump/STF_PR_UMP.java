/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.functions.ump;

import org.actus.attributes.ContractModelProvider;
import org.actus.conventions.businessday.BusinessDayAdjuster;
import org.actus.conventions.contractrole.ContractRoleConvention;
import org.actus.conventions.daycount.DayCountCalculator;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.functions.StateTransitionFunction;
import org.actus.states.StateSpace;
import org.actus.types.ContractRole;

import java.time.LocalDateTime;

public final class STF_PR_UMP implements StateTransitionFunction {
    double payoff=0.0;

    public STF_PR_UMP(double eventPayoff) {
        payoff=eventPayoff;
    }

    @Override
    public StateSpace eval(LocalDateTime time, StateSpace states,
    ContractModelProvider model, RiskFactorModelProvider riskFactorModel, DayCountCalculator dayCounter, BusinessDayAdjuster timeAdjuster) {
        // update state space
        double timeFromLastEvent = dayCounter.dayCountFraction(timeAdjuster.shiftCalcTime(states.statusDate), timeAdjuster.shiftCalcTime(time));
        states.accruedInterest += states.nominalInterestRate * states.notionalPrincipal * timeFromLastEvent;
        states.notionalPrincipal -= ContractRoleConvention.roleSign(model.getAs("contractRole"))*payoff;
        states.feeAccrued += model.<Double>getAs("feeRate") * states.notionalPrincipal * timeFromLastEvent;
        states.statusDate = time;

        // return post-event-states
        return StateSpace.copyStateSpace(states);
    }
    
}
