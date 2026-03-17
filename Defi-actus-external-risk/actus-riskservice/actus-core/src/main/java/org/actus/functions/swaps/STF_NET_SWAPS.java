/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.functions.swaps;

import org.actus.attributes.ContractModelProvider;
import org.actus.conventions.businessday.BusinessDayAdjuster;
import org.actus.conventions.daycount.DayCountCalculator;
import org.actus.events.ContractEvent;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.functions.StateTransitionFunction;
import org.actus.states.StateSpace;

import java.time.LocalDateTime;

public final class STF_NET_SWAPS implements StateTransitionFunction {
    ContractEvent e1;
    ContractEvent e2;

    public STF_NET_SWAPS(ContractEvent e1, ContractEvent e2) {
        this.e1 = e1;
        this.e2 = e2;
    }

    @Override
    public StateSpace eval(LocalDateTime time, StateSpace states,
    ContractModelProvider model, RiskFactorModelProvider riskFactorModel, DayCountCalculator dayCounter, BusinessDayAdjuster timeAdjuster) {
        StateSpace postEventStates = new StateSpace();

        // net post-event-states
        postEventStates.notionalPrincipal = e1.states().notionalPrincipal + e2.states().notionalPrincipal;
        postEventStates.accruedInterest = e1.states().accruedInterest + e2.states().accruedInterest;
        postEventStates.statusDate = time;
        // return post-event-states
        return postEventStates;
    }
    
}
