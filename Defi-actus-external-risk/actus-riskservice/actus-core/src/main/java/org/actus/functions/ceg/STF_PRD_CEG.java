package org.actus.functions.ceg;

import org.actus.attributes.ContractModelProvider;
import org.actus.contracts.CreditEnhancementGuarantee;
import org.actus.conventions.businessday.BusinessDayAdjuster;
import org.actus.conventions.daycount.DayCountCalculator;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.functions.StateTransitionFunction;
import org.actus.states.StateSpace;
import org.actus.util.CommonUtils;

import java.time.LocalDateTime;

public class STF_PRD_CEG implements StateTransitionFunction {
    @Override
    public StateSpace eval(LocalDateTime time, StateSpace states, ContractModelProvider model, RiskFactorModelProvider riskFactorModel, DayCountCalculator dayCounter, BusinessDayAdjuster timeAdjuster) {
        if(CommonUtils.isNull(model.getAs("notionalPrincipal"))){
            states.notionalPrincipal = CreditEnhancementGuarantee.calculateNotionalPrincipal(states, model, riskFactorModel, time);
        }
        if(!CommonUtils.isNull(model.getAs("feeRate"))){
            states.feeAccrued = model.getAs("feeRate");
        } else if(!CommonUtils.isNull(model.getAs("feeAccrued"))){
            states.feeAccrued = model.getAs("feeAccrued");
        }//TODO: implement last two possible initialization
        states.statusDate = time;
        return StateSpace.copyStateSpace(states);
    }
}
