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
import org.actus.util.CommonUtils;

import java.time.LocalDateTime;

public final class POF_MD2_FXOUT implements PayOffFunction {
    
    @Override
    public double eval(LocalDateTime time, StateSpace states, 
                        ContractModelProvider model, RiskFactorModelProvider riskFactorModel, DayCountCalculator dayCounter, BusinessDayAdjuster timeAdjuster) {
        int contractRoleSign = ContractRoleConvention.roleSign(model.getAs("contractRole"));
        return CommonUtils.settlementCurrencyFxRate(riskFactorModel, model, time, states)
                * contractRoleSign
                * (-1)
                * model.<Double>getAs("notionalPrincipal2");
    }
}
