/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.functions.stk;

import org.actus.conventions.contractrole.ContractRoleConvention;
import org.actus.functions.PayOffFunction;
import org.actus.states.StateSpace;
import org.actus.attributes.ContractModelProvider;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.conventions.daycount.DayCountCalculator;
import org.actus.conventions.businessday.BusinessDayAdjuster;
import org.actus.util.CommonUtils;

import java.time.LocalDateTime;

public final class POF_DV_STK implements PayOffFunction {

	@Override
	public double eval(LocalDateTime time, StateSpace states, ContractModelProvider model,
			RiskFactorModelProvider riskFactorModel, DayCountCalculator dayCounter, BusinessDayAdjuster timeAdjuster) {

		/*
		 * If a fixed 'nextDividendPaymentAmount' (DVNP) is provided in the terms,
		 * use that amount for the dividend payoff instead of pulling the value from the
		 * external risk factor model. This allows fixed per-dividend payouts as
		 * specified for the STK contracts.
		 */
		if (!CommonUtils.isNull(model.getAs("nextDividendPaymentAmount"))
				&& (model.<Double>getAs("nextDividendPaymentAmount")) >= 0.0) {
			return CommonUtils.settlementCurrencyFxRate(riskFactorModel, model, time, states)
					* ContractRoleConvention.roleSign(model.getAs("contractRole")) * model.<Double>getAs("quantity")
					* model.<Double>getAs("nextDividendPaymentAmount");
		}

		/*
		 * Default dividend payoff logic: if DVNP is not defined, fall back to
		 * using the market dividend value from the risk factor model. This represents
		 * the standard STK behaviour where dividends come from an observed market
		 * object code rather than a fixed contractual amount.
		 */
		return CommonUtils.settlementCurrencyFxRate(riskFactorModel, model, time, states)
				* ContractRoleConvention.roleSign(model.getAs("contractRole")) * model.<Double>getAs("quantity")
				* riskFactorModel.stateAt(model.getAs("marketObjectCodeOfDividends"), time, states, model, true);
	}
}
