/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.functions.pam;

import org.actus.functions.PayOffFunction;
import org.actus.states.StateSpace;
import org.actus.attributes.ContractModelProvider;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.conventions.daycount.DayCountCalculator;
import org.actus.conventions.businessday.BusinessDayAdjuster;
import org.actus.conventions.contractrole.ContractRoleConvention;
import org.actus.types.ContractRole;
import org.actus.util.CommonUtils;

import java.time.LocalDateTime;

public final class POF_PP_PAM implements PayOffFunction {
    // fnp thinks this code never exercised - ObjectCodeOfPrepaymentModel not in data dictionary - no Events with new STF_PP_LAM Sep2024 
	// mark as behavior model callout 
    // update state space
    @Override
        public double eval(LocalDateTime time, StateSpace states, 
    ContractModelProvider model, RiskFactorModelProvider riskFactorModel, DayCountCalculator dayCounter, BusinessDayAdjuster timeAdjuster) {
        return CommonUtils.settlementCurrencyFxRate(riskFactorModel, model, time, states)
                * ContractRoleConvention.roleSign(model.getAs("contractRole"))
                * riskFactorModel.stateAt(model.getAs("objectCodeOfPrepaymentModel"),time,states,model,false)
                * states.notionalPrincipal;
        }
}
