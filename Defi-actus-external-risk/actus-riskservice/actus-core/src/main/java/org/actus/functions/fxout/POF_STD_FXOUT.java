/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.functions.fxout;

import org.actus.conventions.contractrole.ContractRoleConvention;
import org.actus.functions.PayOffFunction;
import org.actus.states.StateSpace;
import org.actus.attributes.ContractModelProvider;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.conventions.daycount.DayCountCalculator;
import org.actus.conventions.businessday.BusinessDayAdjuster;
import org.actus.types.ContractRole;
import org.actus.util.CommonUtils;

import java.time.LocalDateTime;

public final class POF_STD_FXOUT implements PayOffFunction {
    
    @Override
    public double eval(LocalDateTime time, StateSpace states, 
                        ContractModelProvider model, RiskFactorModelProvider riskFactorModel, DayCountCalculator dayCounter, BusinessDayAdjuster timeAdjuster) {
        double payoff = CommonUtils.settlementCurrencyFxRate(riskFactorModel, model, time, states)
        * ContractRoleConvention.roleSign(model.getAs("contractRole"))
        * (model.<Double>getAs("notionalPrincipal")-riskFactorModel.stateAt(model.getAs("currency2")+"/"+model.getAs("currency"), model.getAs("maturityDate"), states, model,true)
                * model.<Double>getAs("notionalPrincipal2"));
        return payoff;
    }
}
