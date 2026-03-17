package org.actus.functions.optns;

import org.actus.attributes.ContractModelProvider;
import org.actus.conventions.businessday.BusinessDayAdjuster;
import org.actus.conventions.daycount.DayCountCalculator;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.functions.StateTransitionFunction;
import org.actus.states.StateSpace;
import org.actus.types.ContractReference;
import org.actus.types.OptionType;
import org.actus.util.CommonUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class STF_MD_OPTNS implements StateTransitionFunction {
    @Override
    public StateSpace eval(LocalDateTime time, StateSpace states, ContractModelProvider model, RiskFactorModelProvider riskFactorModel, DayCountCalculator dayCounter, BusinessDayAdjuster timeAdjuster) {
        if(CommonUtils.isNull(states.exerciseDate)){
            double x = 0.0;
            double st = riskFactorModel.stateAt(model.<ArrayList<ContractReference>>getAs("contractStructure").get(0).getContractAttribute("marketObjectCode"), time, states, model,true);
            OptionType option = model.getAs("optionType");
            if(option.equals(OptionType.C)){
                x = Math.max(st - model.<Double>getAs("optionStrike1"), 0.0);
            } else if(option.equals(OptionType.P)){
                x = Math.max(model.<Double>getAs("optionStrike1") - st, 0.0);
            } else{
                x = Math.max(st - model.<Double>getAs("optionStrike1"), 0.0) + Math.max(model.<Double>getAs("OptionStrike2") - st, 0.0);
            }
            if(x == 0.0){
                states.exerciseDate = null;
            } else {
                states.exerciseDate = time;
            }
        }
        states.statusDate = time;

        return StateSpace.copyStateSpace(states);
    }
}
