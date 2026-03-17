package org.actus.functions.cec;

import org.actus.attributes.ContractModelProvider;
import org.actus.contracts.CreditEnhancementCollateral;
import org.actus.contracts.CreditEnhancementGuarantee;
import org.actus.conventions.businessday.BusinessDayAdjuster;
import org.actus.conventions.daycount.DayCountCalculator;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.functions.StateTransitionFunction;
import org.actus.states.StateSpace;
import org.actus.util.CommonUtils;
import org.actus.util.CycleUtils;
import org.actus.types.FeeBasis;
import org.actus.conventions.contractrole.ContractRoleConvention;

import java.time.LocalDateTime;

public class STF_XD_CEC implements StateTransitionFunction {
    @Override
    public StateSpace eval(LocalDateTime time, StateSpace states, ContractModelProvider model, RiskFactorModelProvider riskFactorModel, DayCountCalculator dayCounter, BusinessDayAdjuster timeAdjuster) {
        double marketValueCoveringContracts = CreditEnhancementCollateral.calculateMarketValueCoveringContracts(model,riskFactorModel,time);
        states.notionalPrincipal = CreditEnhancementCollateral.calculateNotionalPrincipal(model, riskFactorModel, time);
        states.exerciseAmount = Math.min(marketValueCoveringContracts,states.notionalPrincipal);
        states.exerciseDate = time;
        states.statusDate = time;
        return StateSpace.copyStateSpace(states);
    }
}
