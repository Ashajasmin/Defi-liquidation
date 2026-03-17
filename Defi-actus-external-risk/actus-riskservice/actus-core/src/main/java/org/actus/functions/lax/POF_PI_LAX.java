package org.actus.functions.lax;

import java.time.LocalDateTime;

import org.actus.attributes.ContractModelProvider;
import org.actus.conventions.businessday.BusinessDayAdjuster;
import org.actus.conventions.contractrole.ContractRoleConvention;
import org.actus.conventions.daycount.DayCountCalculator;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.functions.PayOffFunction;
import org.actus.states.StateSpace;
import org.actus.util.CommonUtils;

public class POF_PI_LAX implements PayOffFunction {

private Double prPayment;
	
	public POF_PI_LAX(Double prPayment) {
		this.prPayment=prPayment;
	}

	@Override
	public double eval(LocalDateTime time, StateSpace states, ContractModelProvider model,
			RiskFactorModelProvider riskFactorModel, DayCountCalculator dayCounter, BusinessDayAdjuster timeAdjuster) {
		
		double redemption = prPayment - ContractRoleConvention.roleSign(model.getAs("contractRole"))
						* Math.max(0, Math.abs(prPayment) - Math.abs(states.notionalPrincipal));

		return CommonUtils.settlementCurrencyFxRate(riskFactorModel, model, time, states)
				* (-1)
				* ContractRoleConvention.roleSign(model.getAs("contractRole"))
				* states.notionalScalingMultiplier
				* redemption;
	}

}
