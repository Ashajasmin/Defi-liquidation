package org.actus.functions.swaps;

import org.actus.attributes.ContractModelProvider;
import org.actus.conventions.businessday.BusinessDayAdjuster;
import org.actus.conventions.daycount.DayCountCalculator;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.functions.StateTransitionFunction;
import org.actus.states.StateSpace;

import java.time.LocalDateTime;

public class STF_PRD_SWAPS implements StateTransitionFunction {

    @Override
    public StateSpace eval(LocalDateTime time, StateSpace states,
                           ContractModelProvider model, RiskFactorModelProvider riskFactorModel, DayCountCalculator dayCounter, BusinessDayAdjuster timeAdjuster) {

        // update state space
        states.statusDate = time;

        // return post-event-states
        return StateSpace.copyStateSpace(states);
    }
}
