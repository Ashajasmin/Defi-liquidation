package org.actus.functions.futur;

import org.actus.attributes.ContractModelProvider;
import org.actus.conventions.businessday.BusinessDayAdjuster;
import org.actus.conventions.daycount.DayCountCalculator;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.functions.StateTransitionFunction;
import org.actus.states.StateSpace;
import org.actus.types.ContractReference;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class STF_MD_FUTUR implements StateTransitionFunction {
    @Override
    public StateSpace eval(LocalDateTime time, StateSpace states, ContractModelProvider model, RiskFactorModelProvider riskFactorModel, DayCountCalculator dayCounter, BusinessDayAdjuster timeAdjuster) {
        double st = riskFactorModel.stateAt(model.<ArrayList<ContractReference>>getAs("contractStructure").get(0).getContractAttribute("marketObjectCode"), time, states, model,true);
        double x = st - model.<Double>getAs("futuresPrice");
        if(x == 0.0){
            states.exerciseDate = null;
        } else{
            states.exerciseDate = time;
        }
        states.statusDate = time;

        return StateSpace.copyStateSpace(states);
    }
}
