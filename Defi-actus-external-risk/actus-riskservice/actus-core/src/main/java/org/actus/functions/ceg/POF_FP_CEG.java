package org.actus.functions.ceg;

import org.actus.attributes.ContractModelProvider;
import org.actus.conventions.businessday.BusinessDayAdjuster;
import org.actus.conventions.contractrole.ContractRoleConvention;
import org.actus.conventions.daycount.DayCountCalculator;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.functions.PayOffFunction;
import org.actus.states.StateSpace;
import org.actus.types.FeeBasis;
import org.actus.util.CommonUtils;

import java.time.LocalDateTime;

public class POF_FP_CEG implements PayOffFunction {
    @Override
    public double eval(LocalDateTime time, StateSpace states, ContractModelProvider model, RiskFactorModelProvider riskFactorModel, DayCountCalculator dayCounter, BusinessDayAdjuster timeAdjuster) {
        Double payoff;
        if(FeeBasis.A.equals(model.getAs("feeBasis"))){
            payoff = CommonUtils.settlementCurrencyFxRate(riskFactorModel,model,time,states)
                    * ContractRoleConvention.roleSign(model.getAs("contractRole"))
                    * model.<Double>getAs("feeRate");
        }else{
            double timeFromLastEvent = dayCounter.dayCountFraction(timeAdjuster.shiftCalcTime(states.statusDate), timeAdjuster.shiftCalcTime(time));
            payoff = CommonUtils.settlementCurrencyFxRate(riskFactorModel,model,time,states)
                    * (states.feeAccrued
                    + (states.notionalPrincipal
                    * timeFromLastEvent
                    * model.<Double>getAs("feeRate"))
            );
        }
        return payoff;
    }
}
