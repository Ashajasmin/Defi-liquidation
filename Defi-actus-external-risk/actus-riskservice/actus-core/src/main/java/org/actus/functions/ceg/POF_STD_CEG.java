package org.actus.functions.ceg;

import org.actus.attributes.ContractModelProvider;
import org.actus.conventions.businessday.BusinessDayAdjuster;
import org.actus.conventions.daycount.DayCountCalculator;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.functions.PayOffFunction;
import org.actus.states.StateSpace;
import org.actus.util.CommonUtils;

import java.time.LocalDateTime;

public class POF_STD_CEG implements PayOffFunction {
    @Override
    public double eval(LocalDateTime time, StateSpace states, ContractModelProvider model, RiskFactorModelProvider riskFactorModel, DayCountCalculator dayCounter, BusinessDayAdjuster timeAdjuster) {
        return CommonUtils.settlementCurrencyFxRate(riskFactorModel,model,time,states)
                *(states.exerciseAmount + states.feeAccrued);
    }
}
