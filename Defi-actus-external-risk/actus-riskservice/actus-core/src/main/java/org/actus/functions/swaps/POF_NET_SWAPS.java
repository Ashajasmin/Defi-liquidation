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
import org.actus.functions.PayOffFunction;
import org.actus.states.StateSpace;

import java.time.LocalDateTime;

public final class POF_NET_SWAPS implements PayOffFunction {
    ContractEvent e1;
    ContractEvent e2;

    public POF_NET_SWAPS(ContractEvent e1, ContractEvent e2) {
        this.e1 = e1;
        this.e2 = e2;
    }

    @Override
        public double eval(LocalDateTime time, StateSpace states, 
    ContractModelProvider model, RiskFactorModelProvider riskFactorModel, DayCountCalculator dayCounter, BusinessDayAdjuster timeAdjuster) {
        return e1.payoff() + e2.payoff();
        }
}
