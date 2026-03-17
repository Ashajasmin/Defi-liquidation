package org.actus.functions.lax;

import java.time.LocalDateTime;

import org.actus.attributes.ContractModelProvider;
import org.actus.conventions.businessday.BusinessDayAdjuster;
import org.actus.conventions.contractrole.ContractRoleConvention;
import org.actus.conventions.daycount.DayCountCalculator;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.functions.StateTransitionFunction;
import org.actus.states.StateSpace;

public class STF_PR_LAX implements StateTransitionFunction {

	private double prPayment = 0;

	public STF_PR_LAX(Double prPayment) {
		this.prPayment = prPayment;
	}

	@Override
	public StateSpace eval(LocalDateTime time, StateSpace states, ContractModelProvider model,
			RiskFactorModelProvider riskFactorModel, DayCountCalculator dayCounter, BusinessDayAdjuster timeAdjuster) {
		double role = ContractRoleConvention.roleSign(model.getAs("contractRole"));
		double redemption = role*prPayment - role * Math.max(0, Math.abs(prPayment) - Math.abs(states.notionalPrincipal));

		// update state space
		double timeFromLastEvent = dayCounter.dayCountFraction(timeAdjuster.shiftCalcTime(states.statusDate),
				timeAdjuster.shiftCalcTime(time));
		states.accruedInterest += states.nominalInterestRate * states.interestCalculationBaseAmount * timeFromLastEvent;
		states.feeAccrued += model.<Double>getAs("feeRate") * states.notionalPrincipal * timeFromLastEvent;
		states.notionalPrincipal -= redemption;
		states.statusDate = time;

		// return post-event-states
		return StateSpace.copyStateSpace(states);
	}

}