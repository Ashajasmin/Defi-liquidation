/*
 * Copyright (C) 2016 - present by ACTUS Financial Research Foundation
 *
 * Please see distribution for license.
 */
package org.actus.util;

import org.actus.attributes.ContractModelProvider;
import org.actus.externals.RiskFactorModelProvider;
import org.actus.states.StateSpace;

import java.time.LocalDateTime;

/**
 * Contains general-purpose utilities used throughout the library
 */
public final class CommonUtils {
    
    public static boolean isNull(Object o) {
        return (o == null || String.valueOf(o).equals("NULL"))? true : false;
    }

    /**
     * Calculate the Settlement Currency Rate
     * <p>
     * @param riskFactorModel an external market model
     * @param model the model carrying the contract attributes
     * @param state the inner states of the contract at time
     * @param time the time at which to observe the fx-rate
     * @return the fx-rate
     */
    public static double settlementCurrencyFxRate(RiskFactorModelProvider riskFactorModel, ContractModelProvider model, LocalDateTime time, StateSpace state){
        String settlementCurrency = model.getAs("SettlementCurrency");
        String currency = model.getAs("currency");
        if(isNull(settlementCurrency)  || currency.equals(settlementCurrency)){
            return 1.0;
        }else {
            return riskFactorModel.stateAt(currency + "/" + settlementCurrency, time, state, model,true);
        }
    }
}
