/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.functions.nam;

import java.time.LocalDateTime;

import org.actus.attributes.ContractModelProvider;
import org.actus.conventions.businessday.BusinessDayAdjuster;
import org.actus.conventions.contractrole.ContractRoleConvention;
import org.actus.conventions.daycount.DayCountCalculator;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.functions.StateTransitionFunction;
import org.actus.states.StateSpace;

public final class STF_PR2_NAM implements StateTransitionFunction {

	@Override
	public StateSpace eval(LocalDateTime time, StateSpace states, ContractModelProvider model,
			RiskFactorModelProvider riskFactorModel, DayCountCalculator dayCounter, BusinessDayAdjuster timeAdjuster) {
		// update state space
		double timeFromLastEvent = dayCounter.dayCountFraction(timeAdjuster.shiftCalcTime(states.statusDate), timeAdjuster.shiftCalcTime(time));
		states.accruedInterest += states.nominalInterestRate * states.interestCalculationBaseAmount * timeFromLastEvent;
		states.feeAccrued += model.<Double>getAs("feeRate") * states.notionalPrincipal * timeFromLastEvent;
		double redemptionAmount = states.nextPrincipalRedemptionPayment - ContractRoleConvention.roleSign(model.getAs("contractRole")) * states.accruedInterest;
        double redemption = redemptionAmount - Math.max(0, redemptionAmount - Math.abs(states.notionalPrincipal));
		states.notionalPrincipal -= ContractRoleConvention.roleSign(model.getAs("contractRole")) * redemption;
        states.interestCalculationBaseAmount = states.notionalPrincipal;
		states.statusDate = time;

		// return post-event-states
		return StateSpace.copyStateSpace(states);
	}

}
