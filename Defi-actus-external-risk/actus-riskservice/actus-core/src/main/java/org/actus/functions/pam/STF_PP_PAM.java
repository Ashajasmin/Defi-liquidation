/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.functions.pam;

import org.actus.functions.StateTransitionFunction;
import org.actus.states.StateSpace;
import org.actus.attributes.ContractModelProvider;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.conventions.daycount.DayCountCalculator;
import org.actus.conventions.businessday.BusinessDayAdjuster;

import java.time.LocalDateTime;

public final class STF_PP_PAM implements StateTransitionFunction {
    // fnp thinks this code never exercised - ObjectCodeOfPrepaymentModel not in data dictionary - no Events with new STF_PP_LAM Sep2024 
    // mark as behavior model stateAt() callout - unknown model 
    @Override
    public StateSpace eval(LocalDateTime time, StateSpace states,
    ContractModelProvider model, RiskFactorModelProvider riskFactorModel, DayCountCalculator dayCounter, BusinessDayAdjuster timeAdjuster) {
        // update state space
        double timeFromLastEvent = dayCounter.dayCountFraction(timeAdjuster.shiftCalcTime(states.statusDate), timeAdjuster.shiftCalcTime(time));
        states.accruedInterest += states.nominalInterestRate * states.notionalPrincipal * timeFromLastEvent;
        states.feeAccrued += model.<Double>getAs("feeRate") * states.notionalPrincipal * timeFromLastEvent;
        states.notionalPrincipal -= riskFactorModel.stateAt(model.getAs("objectCodeOfPrepaymentModel"),time,states,model,false) * states.notionalPrincipal;
        states.statusDate = time;

        // return post-event-states
        return StateSpace.copyStateSpace(states);
    }
}
