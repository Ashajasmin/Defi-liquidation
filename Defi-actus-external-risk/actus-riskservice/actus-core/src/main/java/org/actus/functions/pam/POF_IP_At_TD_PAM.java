package org.actus.functions.pam;

import java.time.LocalDateTime;

import org.actus.attributes.ContractModelProvider;
import org.actus.conventions.businessday.BusinessDayAdjuster;
import org.actus.conventions.contractrole.ContractRoleConvention;
import org.actus.conventions.daycount.DayCountCalculator;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.functions.PayOffFunction;
import org.actus.states.StateSpace;
import org.actus.util.CommonUtils;

public class POF_IP_At_TD_PAM implements PayOffFunction {

	@Override
	public double eval(LocalDateTime time, StateSpace states, ContractModelProvider model,
			RiskFactorModelProvider riskFactorModel, DayCountCalculator dayCounter, BusinessDayAdjuster timeAdjuster) {
		 return CommonUtils.settlementCurrencyFxRate(riskFactorModel, model, time, states) * ContractRoleConvention.roleSign(model.getAs("contractRole"))
	        		* states.interestScalingMultiplier
	                * (states.accruedInterest + (dayCounter.dayCountFraction(timeAdjuster.shiftCalcTime(states.statusDate), timeAdjuster.shiftCalcTime(time))
	                * states.nominalInterestRate
	                * states.notionalPrincipal));
	        
	}

}
