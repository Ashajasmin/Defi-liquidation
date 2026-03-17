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
import org.actus.types.PenaltyType;
import org.actus.util.CommonUtils;

import java.time.LocalDateTime;

public final class POF_PY_PAM implements PayOffFunction {
    
    @Override
    public double eval(LocalDateTime time, StateSpace states, 
    ContractModelProvider model, RiskFactorModelProvider riskFactorModel, DayCountCalculator dayCounter, BusinessDayAdjuster timeAdjuster) {
        if(model.getAs("penaltyType").equals(PenaltyType.A)) {
            return CommonUtils.settlementCurrencyFxRate(riskFactorModel, model, time, states)
                    * ContractRoleConvention.roleSign(model.getAs("contractRole")) * model.<Double>getAs("penaltyRate");
        } else if(model.getAs("penaltyType").equals(PenaltyType.N)) {
            return CommonUtils.settlementCurrencyFxRate(riskFactorModel, model, time, states)
                    * ContractRoleConvention.roleSign(model.getAs("contractRole"))
                    * dayCounter.dayCountFraction(timeAdjuster.shiftCalcTime(states.statusDate), timeAdjuster.shiftCalcTime(time))
                    * model.<Double>getAs("penaltyRate")
                    * states.notionalPrincipal;
        } else {
            return CommonUtils.settlementCurrencyFxRate(riskFactorModel, model, time, states)
                    * ContractRoleConvention.roleSign(model.getAs("contractRole"))
                    * dayCounter.dayCountFraction(timeAdjuster.shiftCalcTime(states.statusDate), timeAdjuster.shiftCalcTime(time))
                    * states.notionalPrincipal
                    * Math.max(0, states.nominalInterestRate - riskFactorModel.stateAt(model.getAs("marketObjectCodeOfRateReset"), time,states,model,true));
        }
    }
}
