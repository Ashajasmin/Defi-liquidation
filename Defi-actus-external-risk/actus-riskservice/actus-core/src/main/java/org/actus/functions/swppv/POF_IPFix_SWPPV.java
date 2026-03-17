/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.functions.swppv;

import org.actus.functions.PayOffFunction;
import org.actus.states.StateSpace;
import org.actus.attributes.ContractModelProvider;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.conventions.daycount.DayCountCalculator;
import org.actus.conventions.businessday.BusinessDayAdjuster;
import org.actus.util.CommonUtils;

import java.time.LocalDateTime;

public final class POF_IPFix_SWPPV implements PayOffFunction {
    
    @Override
        public double eval(LocalDateTime time, StateSpace states, 
    ContractModelProvider model, RiskFactorModelProvider riskFactorModel, DayCountCalculator dayCounter, BusinessDayAdjuster timeAdjuster) {
        double timeFromLastEvent = dayCounter.dayCountFraction(timeAdjuster.shiftCalcTime(states.statusDate), timeAdjuster.shiftCalcTime(time));
        return CommonUtils.settlementCurrencyFxRate(riskFactorModel, model, time, states)
                * (states.accruedInterest + timeFromLastEvent * model.<Double>getAs("nominalInterestRate") * states.notionalPrincipal);
        }
}
